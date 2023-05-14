package me.rhunk.snapenhance.data

enum class MessageState {
    PREPARING, SENDING, COMMITTED, FAILED, CANCELING
}

enum class ContentType(val id: Int) {
    UNKNOWN(-1),
    SNAP(0),
    CHAT(1),
    EXTERNAL_MEDIA(2),
    SHARE(3),
    NOTE(4),
    STICKER(5),
    STATUS(6),
    LOCATION(7),
    STATUS_SAVE_TO_CAMERA_ROLL(8),
    STATUS_CONVERSATION_CAPTURE_SCREENSHOT(9),
    STATUS_CONVERSATION_CAPTURE_RECORD(10),
    STATUS_CALL_MISSED_VIDEO(11),
    STATUS_CALL_MISSED_AUDIO(12),
    LIVE_LOCATION_SHARE(13),
    CREATIVE_TOOL_ITEM(14),
    FAMILY_CENTER_INVITE(15),
    FAMILY_CENTER_ACCEPT(16),
    FAMILY_CENTER_LEAVE(17);

    companion object {
        fun fromId(i: Int): ContentType {
            return values().firstOrNull { it.id == i } ?: UNKNOWN
        }
    }
}

enum class PlayableSnapState {
    NOTDOWNLOADED, DOWNLOADING, DOWNLOADFAILED, PLAYABLE, VIEWEDREPLAYABLE, PLAYING, VIEWEDNOTREPLAYABLE
}

enum class MediaReferenceType {
    UNASSIGNED, OVERLAY, IMAGE, VIDEO, ASSET_BUNDLE, AUDIO, ANIMATED_IMAGE, FONT, WEB_VIEW_CONTENT, VIDEO_NO_AUDIO
}
