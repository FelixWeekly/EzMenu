package com.ezmenu.app.ui.icons

/**
 * 设计稿用到的 Material Symbols 字形。
 *
 * 由 `tools/gen_msicons.py` 从官方 `.codepoints` 表生成 —— **请勿手改**，
 * 需要增删字形时改 `tools/fetch_assets.py` 里的 GLYPHS 列表并重新生成。
 *
 * 字体只保留了这些码点（见 `tools/ezmenu_glyphs.txt`），共 59 个。
 * 码点均位于 BMP 私用区，每个字形对应单个码位，可直接用 [text] 交给 Text 渲染。
 */
enum class MsGlyph(val glyph: String, val codePoint: Int) {
    Add("add", 0xE145),
    AddPhotoAlternate("add_photo_alternate", 0xE43E),
    ArrowBack("arrow_back", 0xE5C4),
    ArrowDownward("arrow_downward", 0xE5DB),
    ArrowForward("arrow_forward", 0xE5C8),
    ArrowUpward("arrow_upward", 0xE5D8),
    AutoAwesome("auto_awesome", 0xE65F),
    Bookmark("bookmark", 0xE8E7),
    BookmarkBorder("bookmark_border", 0xE8E7),
    BookmarkHeart("bookmark_heart", 0xF455),
    Cancel("cancel", 0xE888),
    ChangeCircle("change_circle", 0xE2E7),
    Check("check", 0xE668),
    CheckCircle("check_circle", 0xF0BE),
    ChevronRight("chevron_right", 0xE5CC),
    Close("close", 0xE5CD),
    ContentCopy("content_copy", 0xE14D),
    Cooking("cooking", 0xE2B6),
    Delete("delete", 0xE92E),
    DownloadForOffline("download_for_offline", 0xF000),
    Drafts("drafts", 0xE151),
    DragIndicator("drag_indicator", 0xE945),
    Explore("explore", 0xE87A),
    Favorite("favorite", 0xE87E),
    FavoriteBorder("favorite_border", 0xE87E),
    FrontHand("front_hand", 0xE769),
    GridView("grid_view", 0xE9B0),
    Group("group", 0xEA21),
    HistoryEdu("history_edu", 0xEA3E),
    HourglassTop("hourglass_top", 0xEA5B),
    Info("info", 0xE88E),
    Kitchen("kitchen", 0xEB47),
    LightMode("light_mode", 0xE518),
    Lightbulb("lightbulb", 0xE90F),
    LocalFireDepartment("local_fire_department", 0xEF55),
    MagicButton("magic_button", 0xF136),
    MenuBook("menu_book", 0xEA19),
    Mic("mic", 0xE31D),
    Notifications("notifications", 0xE7F5),
    Pause("pause", 0xE034),
    Person("person", 0xF0D3),
    PersonOutline("person_outline", 0xF0D3),
    PhotoCamera("photo_camera", 0xE412),
    PlayArrow("play_arrow", 0xE037),
    PlayCircle("play_circle", 0xE1C4),
    Remove("remove", 0xE15B),
    Replay("replay", 0xE042),
    RestartAlt("restart_alt", 0xF053),
    Scale("scale", 0xEB5F),
    Schedule("schedule", 0xEFD6),
    Search("search", 0xEF7A),
    Share("share", 0xE80D),
    Skillet("skillet", 0xF543),
    SoupKitchen("soup_kitchen", 0xE7D3),
    Spa("spa", 0xEB4C),
    Timer("timer", 0xE425),
    TipsAndUpdates("tips_and_updates", 0xE79A),
    Tune("tune", 0xE429),
    WbSunny("wb_sunny", 0xE430),
    ;

    /** 供 Compose `Text` 渲染的单字符。 */
    val text: String = String(Character.toChars(codePoint))
}
