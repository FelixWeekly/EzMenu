package com.ezmenu.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ezmenu.app.data.local.EzMenuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.*

/** Portable, checked data archive. Running cooking sessions are intentionally excluded. */
class ArchiveRepository(private val context: Context,private val db: EzMenuDatabase,private val settings: SettingsRepository) {
    private val tables=listOf("recipes","ingredient_groups","ingredients","steps","tags","recipe_tags","cook_logs","pantry_items","editor_drafts","app_meta")
    private val pending=File(context.filesDir,"restore.pending")
    private val transientRollback=File(context.filesDir,"restore-sessions.json")
    private val rollback=File(context.filesDir,"restore-rollback.ezmenu")
    data class Inspection(val recipes: Int,val entries: Int,val photos: Int)
    private fun digest(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private suspend fun snapshot(selected: List<String> = tables): JSONObject=db.withTransaction {
        JSONObject().apply { selected.forEach { table ->
            val rows=JSONArray()
            db.openHelper.writableDatabase.query("SELECT * FROM "+table).use { c -> while(c.moveToNext()){
                val row=JSONObject();c.columnNames.forEachIndexed { i,name -> row.put(name,when(c.getType(i)){
                    android.database.Cursor.FIELD_TYPE_NULL->JSONObject.NULL
                    android.database.Cursor.FIELD_TYPE_INTEGER->c.getLong(i)
                    android.database.Cursor.FIELD_TYPE_FLOAT->c.getDouble(i)
                    else->c.getString(i)
                }) };rows.put(row)
            } };put(table,rows)
        } }
    }
    suspend fun export(target: File): File=withContext(Dispatchers.IO) {
        val data=JSONObject().put("tables",snapshot()).put("settings",settings.exportSettings()).toString().toByteArray()
        val photos=File(context.filesDir,"covers").listFiles()?.filter{it.isFile}?:emptyList()
        val checks=JSONObject().put("data.json",digest(data))
        target.parentFile?.mkdirs()
        try { ZipOutputStream(target.outputStream().buffered()).use { zip ->
            fun write(name: String, bytes: ByteArray){zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()}
            write("data.json",data)
            photos.forEach { val bytes=it.readBytes();val name="covers/"+it.name;checks.put(name,digest(bytes));write(name,bytes) }
            write("manifest.json",JSONObject().put("format",2).put("checksums",checks).toString().toByteArray())
        };target } catch(e:Exception){target.delete();throw e}
    }
    private fun unpack(file: File): Map<String,ByteArray> {
        val files=linkedMapOf<String,ByteArray>();var total=0L
        ZipInputStream(file.inputStream().buffered()).use{zip->while(true){val entry=zip.nextEntry?:break
            val name=entry.name
            require(name=="data.json"||name=="manifest.json"||(name.startsWith("covers/")&&name.substringAfter("covers/").matches(Regex("[A-Za-z0-9_.-]+"))&&!name.contains(".."))){"备份包含无效路径"}
            require(name !in files && files.size<30000){"备份包含重复或过多文件"}
            val bytes=ByteArrayOutputStream();val buffer=ByteArray(16384);var n=zip.read(buffer)
            while(n>=0){total+=n;require(total<=512L*1024*1024 && bytes.size()+n<=32*1024*1024){"备份过大"};bytes.write(buffer,0,n);n=zip.read(buffer)}
            files[name]=bytes.toByteArray();zip.closeEntry()
        }}
        val manifest=JSONObject(String(requireNotNull(files["manifest.json"]){"缺少备份清单"},Charsets.UTF_8))
        require(manifest.getInt("format")==2){"不支持的备份版本"}
        val hashes=manifest.getJSONObject("checksums")
        require(hashes.keys().asSequence().toSet()==files.keys.minus("manifest.json")){"备份文件清单不完整"}
        hashes.keys().forEach{require(digest(requireNotNull(files[it]))==hashes.getString(it)){"备份校验失败"}}
        val data=JSONObject(String(requireNotNull(files["data.json"]),Charsets.UTF_8)).getJSONObject("tables")
        require(data.keys().asSequence().toSet()==tables.toSet()){"备份数据表不完整"}
        return files
    }
    suspend fun inspect(file: File): Inspection=withContext(Dispatchers.IO){val files=unpack(file);val d=JSONObject(String(files.getValue("data.json"),Charsets.UTF_8)).getJSONObject("tables")
        Inspection(d.getJSONArray("recipes").length(),d.getJSONArray("cook_logs").length(),files.keys.count{it.startsWith("covers/")})}
    private suspend fun apply(files: Map<String,ByteArray>) {
        val root=JSONObject(String(files.getValue("data.json"),Charsets.UTF_8))
        db.withTransaction {
            val sql=db.openHelper.writableDatabase
            sql.execSQL("DELETE FROM cooking_timers");sql.execSQL("DELETE FROM cooking_sessions")
            tables.reversed().forEach{sql.execSQL("DELETE FROM "+it)}
            tables.forEach { table ->
                val columns=mutableListOf<String>();sql.query("PRAGMA table_info("+table+")").use{c->while(c.moveToNext())columns+=c.getString(1)}
                val rows=root.getJSONObject("tables").getJSONArray(table)
                for(i in 0 until rows.length()){
                    val row=rows.getJSONObject(i);require(row.keys().asSequence().toSet()==columns.toSet()){"备份字段不匹配"}
                    val args=columns.map{key->if(row.isNull(key))null else row.get(key)}.toTypedArray()
                    sql.execSQL("INSERT INTO "+table+" ("+columns.joinToString(",")+") VALUES ("+columns.joinToString(","){"?"}+")",args)
                }
            }
            sql.query("PRAGMA foreign_key_check").use{require(!it.moveToFirst()){"备份关联数据损坏"}}
        }
        files.filterKeys{it.startsWith("covers/")}.forEach{(name,bytes)->
            val file=File(context.filesDir,name);file.parentFile?.mkdirs();val temp=File(file.parentFile,file.name+".restore");temp.writeBytes(bytes)
            check(temp.renameTo(file)){"照片恢复失败"}
        }
        settings.importSettings(root.getJSONObject("settings"))
    }
    suspend fun restore(file: File)=withContext(Dispatchers.IO){
        val verified=unpack(file)
        export(rollback)
        transientRollback.writeText(snapshot(listOf("cooking_sessions","cooking_timers")).toString())
        pending.writeText("rollback")
        try { apply(verified);check(pending.delete());transientRollback.delete() }
        catch(e:Exception){try{apply(unpack(rollback));restoreTransient();pending.delete();transientRollback.delete()}catch(recovery:Exception){e.addSuppressed(recovery)};throw e}
    }
    private suspend fun restoreTransient() {
        if(!transientRollback.isFile)return
        val root=JSONObject(transientRollback.readText())
        db.withTransaction {
            val sql=db.openHelper.writableDatabase
            listOf("cooking_sessions","cooking_timers").forEach{table->
                val rows=root.getJSONArray(table)
                for(i in 0 until rows.length()){
                    val row=rows.getJSONObject(i);val columns=row.keys().asSequence().toList()
                    sql.execSQL("INSERT OR REPLACE INTO "+table+" ("+columns.joinToString(",")+") VALUES ("+columns.joinToString(","){"?"}+")",columns.map{if(row.isNull(it))null else row.get(it)}.toTypedArray())
                }
            }
        }
    }
    suspend fun recoverIfNeeded()=withContext(Dispatchers.IO){if(pending.exists()){apply(unpack(rollback));restoreTransient();check(pending.delete());transientRollback.delete()}}

}
