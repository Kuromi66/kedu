package com.pulse.checkin.ui.components

val habitIconOptions = listOf(
    HabitIconPreset(token = ":dumbbell", label = "\u8fd0\u52a8\u5065\u8eab"),
    HabitIconPreset(token = ":book-open", label = "\u9605\u8bfb"),
    HabitIconPreset(token = ":droplet", label = "\u559d\u6c34"),
    HabitIconPreset(token = ":brain", label = "\u51a5\u60f3"),
    HabitIconPreset(token = ":moon", label = "\u65e9\u7761"),
    HabitIconPreset(token = ":graduation-cap", label = "\u5b66\u4e60"),
    HabitIconPreset(token = ":pencil", label = "\u5199\u4f5c"),
    HabitIconPreset(token = ":footprints", label = "\u8dd1\u6b65"),
    HabitIconPreset(token = ":apple", label = "\u5065\u5eb7\u996e\u98df"),
    HabitIconPreset(token = ":heart-pulse", label = "\u953b\u70bc"),
    HabitIconPreset(token = ":coffee", label = "\u65e9\u9910"),
    HabitIconPreset(token = ":music", label = "\u7ec3\u4e60\u4e50\u5668"),
    HabitIconPreset(token = ":palette", label = "\u7ed8\u753b"),
    HabitIconPreset(token = ":message-circle", label = "\u793e\u4ea4"),
    HabitIconPreset(token = ":sun", label = "\u65e9\u8d77"),
    HabitIconPreset(token = ":wind", label = "\u6df1\u547c\u5438"),
    HabitIconPreset(token = ":flower-2", label = "\u56ed\u827a"),
    HabitIconPreset(token = ":target", label = "\u76ee\u6807\u89c4\u5212"),
    HabitIconPreset(token = ":sparkles", label = "\u62a4\u80a4"),
    HabitIconPreset(token = ":smile", label = "\u6bcf\u65e5\u5fae\u7b11"),
    HabitIconPreset(token = ":bike", label = "\u9a91\u884c"),
    HabitIconPreset(token = ":waves", label = "\u6e38\u6cf3"),
    HabitIconPreset(token = ":activity", label = "\u745c\u4f3d"),
    HabitIconPreset(token = ":move-vertical", label = "\u62c9\u4f38"),
    HabitIconPreset(token = ":mountain", label = "\u722c\u5c71"),
    HabitIconPreset(token = ":code", label = "\u7f16\u7a0b"),
    HabitIconPreset(token = ":languages", label = "\u5b66\u5916\u8bed"),
    HabitIconPreset(token = ":pen-tool", label = "\u7ec3\u5b57"),
    HabitIconPreset(token = ":book-text", label = "\u8bb0\u65e5\u8bb0"),
    HabitIconPreset(token = ":home", label = "\u6574\u7406\u623f\u95f4"),
    HabitIconPreset(token = ":coins", label = "\u7701\u94b1\u7406\u8d22"),
    HabitIconPreset(token = ":timer", label = "\u65ad\u98df"),
    HabitIconPreset(token = ":cigarette-off", label = "\u6212\u70df"),
    HabitIconPreset(token = ":stethoscope", label = "\u4f53\u68c0"),
    HabitIconPreset(token = ":pill", label = "\u5403\u836f\u63d0\u9192"),
    HabitIconPreset(token = ":film", label = "\u770b\u7535\u5f71"),
    HabitIconPreset(token = ":gamepad-2", label = "\u73a9\u6e38\u620f"),
    HabitIconPreset(token = ":camera", label = "\u6444\u5f71"),
    HabitIconPreset(token = ":users", label = "\u5bb6\u5ead\u65f6\u95f4"),
    HabitIconPreset(token = ":user-plus", label = "\u62dc\u8bbf\u670b\u53cb"),
    HabitIconPreset(token = ":hand-heart", label = "\u5fd7\u613f\u670d\u52a1"),
    HabitIconPreset(token = ":file-text", label = "\u5199\u5468\u62a5"),
    HabitIconPreset(token = ":video", label = "\u5f00\u4f1a"),
    HabitIconPreset(token = ":clipboard-check", label = "\u590d\u76d8\u603b\u7ed3")
)



data class HabitIconCategory(
    val key: String,
    val options: List<HabitIconPreset>,
)

private fun iconOptionsOf(vararg tokens: String): List<HabitIconPreset> =
    tokens.mapNotNull { token -> habitIconOptions.find { it.token == token } }

val habitIconCategories = listOf(
    HabitIconCategory(
        key = "health",
        options = iconOptionsOf(
            ":dumbbell", ":droplet", ":brain", ":moon", ":footprints", ":apple", ":heart-pulse", ":sun",
            ":wind", ":sparkles", ":smile", ":bike", ":waves", ":activity", ":move-vertical", ":mountain",
            ":timer", ":cigarette-off", ":stethoscope", ":pill"
        ),
    ),
    HabitIconCategory(
        key = "growth",
        options = iconOptionsOf(
            ":book-open", ":graduation-cap", ":pencil", ":music", ":palette", ":flower-2", ":target", ":code",
            ":languages", ":pen-tool", ":book-text", ":file-text", ":clipboard-check"
        ),
    ),
    HabitIconCategory(
        key = "life",
        options = iconOptionsOf(
            ":coffee", ":home", ":coins", ":film", ":gamepad-2", ":camera"
        ),
    ),
    HabitIconCategory(
        key = "social",
        options = iconOptionsOf(
            ":message-circle", ":users", ":user-plus", ":hand-heart", ":video"
        ),
    ),
)

private val habitIconNodes = mapOf(
    ":dumbbell" to listOf(
            LucidePathNode("M14.4 14.4 9.6 9.6"),
            LucidePathNode("M18.657 21.485a2 2 0 1 1-2.829-2.828l-1.767 1.768a2 2 0 1 1-2.829-2.829l6.364-6.364a2 2 0 1 1 2.829 2.829l-1.768 1.767a2 2 0 1 1 2.828 2.829z"),
            LucidePathNode("m21.5 21.5-1.4-1.4"),
            LucidePathNode("M3.9 3.9 2.5 2.5"),
            LucidePathNode("M6.404 12.768a2 2 0 1 1-2.829-2.829l1.768-1.767a2 2 0 1 1-2.828-2.829l2.828-2.828a2 2 0 1 1 2.829 2.828l1.767-1.768a2 2 0 1 1 2.829 2.829z")
        ),
    ":book-open" to listOf(
            LucidePathNode("M12 7v14"),
            LucidePathNode("M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z")
        ),
    ":droplet" to listOf(
            LucidePathNode("M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
        ),
    ":brain" to listOf(
            LucidePathNode("M12 5a3 3 0 1 0-5.997.125 4 4 0 0 0-2.526 5.77 4 4 0 0 0 .556 6.588A4 4 0 1 0 12 18Z"),
            LucidePathNode("M12 5a3 3 0 1 1 5.997.125 4 4 0 0 1 2.526 5.77 4 4 0 0 1-.556 6.588A4 4 0 1 1 12 18Z"),
            LucidePathNode("M15 13a4.5 4.5 0 0 1-3-4 4.5 4.5 0 0 1-3 4"),
            LucidePathNode("M17.599 6.5a3 3 0 0 0 .399-1.375"),
            LucidePathNode("M6.003 5.125A3 3 0 0 0 6.401 6.5"),
            LucidePathNode("M3.477 10.896a4 4 0 0 1 .585-.396"),
            LucidePathNode("M19.938 10.5a4 4 0 0 1 .585.396"),
            LucidePathNode("M6 18a4 4 0 0 1-1.967-.516"),
            LucidePathNode("M19.967 17.484A4 4 0 0 1 18 18")
        ),
    ":moon" to listOf(
            LucidePathNode("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
        ),
    ":graduation-cap" to listOf(
            LucidePathNode("M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"),
            LucidePathNode("M22 10v6"),
            LucidePathNode("M6 12.5V16a6 3 0 0 0 12 0v-3.5")
        ),
    ":pencil" to listOf(
            LucidePathNode("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"),
            LucidePathNode("m15 5 4 4")
        ),
    ":footprints" to listOf(
            LucidePathNode("M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0Z"),
            LucidePathNode("M20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0Z"),
            LucidePathNode("M16 17h4"),
            LucidePathNode("M4 13h4")
        ),
    ":apple" to listOf(
            LucidePathNode("M12 20.94c1.5 0 2.75 1.06 4 1.06 3 0 6-8 6-12.22A4.91 4.91 0 0 0 17 5c-2.22 0-4 1.44-5 2-1-.56-2.78-2-5-2a4.9 4.9 0 0 0-5 4.78C2 14 5 22 8 22c1.25 0 2.5-1.06 4-1.06Z"),
            LucidePathNode("M10 2c1 .5 2 2 2 5")
        ),
    ":heart-pulse" to listOf(
            LucidePathNode("M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"),
            LucidePathNode("M3.22 12H9.5l.5-1 2 4.5 2-7 1.5 3.5h5.27")
        ),
    ":coffee" to listOf(
            LucidePathNode("M10 2v2"),
            LucidePathNode("M14 2v2"),
            LucidePathNode("M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"),
            LucidePathNode("M6 2v2")
        ),
    ":music" to listOf(
            LucidePathNode("M9 18V5l12-2v13"),
            LucideCircleNode(cx = 6f, cy = 18f, r = 3f),
            LucideCircleNode(cx = 18f, cy = 16f, r = 3f)
        ),
    ":palette" to listOf(
            LucideCircleNode(cx = 13.5f, cy = 6.5f, r = 0.5f),
            LucideCircleNode(cx = 17.5f, cy = 10.5f, r = 0.5f),
            LucideCircleNode(cx = 8.5f, cy = 7.5f, r = 0.5f),
            LucideCircleNode(cx = 6.5f, cy = 12.5f, r = 0.5f),
            LucidePathNode("M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2z")
        ),
    ":message-circle" to listOf(
            LucidePathNode("M7.9 20A9 9 0 1 0 4 16.1L2 22Z")
        ),
    ":sun" to listOf(
            LucideCircleNode(cx = 12f, cy = 12f, r = 4f),
            LucidePathNode("M12 2v2"),
            LucidePathNode("M12 20v2"),
            LucidePathNode("m4.93 4.93 1.41 1.41"),
            LucidePathNode("m17.66 17.66 1.41 1.41"),
            LucidePathNode("M2 12h2"),
            LucidePathNode("M20 12h2"),
            LucidePathNode("m6.34 17.66-1.41 1.41"),
            LucidePathNode("m19.07 4.93-1.41 1.41")
        ),
    ":wind" to listOf(
            LucidePathNode("M12.8 19.6A2 2 0 1 0 14 16H2"),
            LucidePathNode("M17.5 8a2.5 2.5 0 1 1 2 4H2"),
            LucidePathNode("M9.8 4.4A2 2 0 1 1 11 8H2")
        ),
    ":flower-2" to listOf(
            LucidePathNode("M12 5a3 3 0 1 1 3 3m-3-3a3 3 0 1 0-3 3m3-3v1M9 8a3 3 0 1 0 3 3M9 8h1m5 0a3 3 0 1 1-3 3m3-3h-1m-2 3v-1"),
            LucideCircleNode(cx = 12f, cy = 8f, r = 2f),
            LucidePathNode("M12 10v12"),
            LucidePathNode("M12 22c4.2 0 7-1.667 7-5-4.2 0-7 1.667-7 5Z"),
            LucidePathNode("M12 22c-4.2 0-7-1.667-7-5 4.2 0 7 1.667 7 5Z")
        ),
    ":target" to listOf(
            LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
            LucideCircleNode(cx = 12f, cy = 12f, r = 6f),
            LucideCircleNode(cx = 12f, cy = 12f, r = 2f)
        ),
    ":sparkles" to listOf(
            LucidePathNode("M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"),
            LucidePathNode("M20 3v4"),
            LucidePathNode("M22 5h-4"),
            LucidePathNode("M4 17v2"),
            LucidePathNode("M5 18H3")
        ),
    ":smile" to listOf(
            LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
            LucidePathNode("M8 14s1.5 2 4 2 4-2 4-2"),
            LucideLineNode(x1 = 9f, y1 = 9f, x2 = 9.01f, y2 = 9f),
            LucideLineNode(x1 = 15f, y1 = 9f, x2 = 15.01f, y2 = 9f)
        ),
    ":bike" to listOf(
            LucideCircleNode(cx = 18.5f, cy = 17.5f, r = 3.5f),
            LucideCircleNode(cx = 5.5f, cy = 17.5f, r = 3.5f),
            LucideCircleNode(cx = 15f, cy = 5f, r = 1f),
            LucidePathNode("M12 17.5V14l-3-3 4-3 2 3h2")
        ),
    ":waves" to listOf(
            LucidePathNode("M2 6c.6.5 1.2 1 2.5 1C7 7 7 5 9.5 5c2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1"),
            LucidePathNode("M2 12c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1"),
            LucidePathNode("M2 18c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1")
        ),
    ":activity" to listOf(
            LucidePathNode("M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2")
        ),
    ":move-vertical" to listOf(
            LucidePathNode("M12 2v20"),
            LucidePathNode("m8 18 4 4 4-4"),
            LucidePathNode("m8 6 4-4 4 4")
        ),
    ":mountain" to listOf(
            LucidePathNode("m8 3 4 8 5-5 5 15H2L8 3z")
        ),
    ":code" to listOf(
            LucidePolylineNode(points = listOf(LucidePoint(16f, 18f), LucidePoint(22f, 12f), LucidePoint(16f, 6f))),
            LucidePolylineNode(points = listOf(LucidePoint(8f, 6f), LucidePoint(2f, 12f), LucidePoint(8f, 18f)))
        ),
    ":languages" to listOf(
            LucidePathNode("m5 8 6 6"),
            LucidePathNode("m4 14 6-6 2-3"),
            LucidePathNode("M2 5h12"),
            LucidePathNode("M7 2h1"),
            LucidePathNode("m22 22-5-10-5 10"),
            LucidePathNode("M14 18h6")
        ),
    ":pen-tool" to listOf(
            LucidePathNode("M15.707 21.293a1 1 0 0 1-1.414 0l-1.586-1.586a1 1 0 0 1 0-1.414l5.586-5.586a1 1 0 0 1 1.414 0l1.586 1.586a1 1 0 0 1 0 1.414z"),
            LucidePathNode("m18 13-1.375-6.874a1 1 0 0 0-.746-.776L3.235 2.028a1 1 0 0 0-1.207 1.207L5.35 15.879a1 1 0 0 0 .776.746L13 18"),
            LucidePathNode("m2.3 2.3 7.286 7.286"),
            LucideCircleNode(cx = 11f, cy = 11f, r = 2f)
        ),
    ":book-text" to listOf(
            LucidePathNode("M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20"),
            LucidePathNode("M8 11h8"),
            LucidePathNode("M8 7h6")
        ),
    ":home" to listOf(
            LucidePathNode("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"),
            LucidePathNode("M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
        ),
    ":coins" to listOf(
            LucideCircleNode(cx = 8f, cy = 8f, r = 6f),
            LucidePathNode("M18.09 10.37A6 6 0 1 1 10.34 18"),
            LucidePathNode("M7 6h1v4"),
            LucidePathNode("m16.71 13.88.7.71-2.82 2.82")
        ),
    ":timer" to listOf(
            LucideLineNode(x1 = 10f, y1 = 2f, x2 = 14f, y2 = 2f),
            LucideLineNode(x1 = 12f, y1 = 14f, x2 = 15f, y2 = 11f),
            LucideCircleNode(cx = 12f, cy = 14f, r = 8f)
        ),
    ":cigarette-off" to listOf(
            LucidePathNode("M12 12H3a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h13"),
            LucidePathNode("M18 8c0-2.5-2-2.5-2-5"),
            LucidePathNode("m2 2 20 20"),
            LucidePathNode("M21 12a1 1 0 0 1 1 1v2a1 1 0 0 1-.5.866"),
            LucidePathNode("M22 8c0-2.5-2-2.5-2-5"),
            LucidePathNode("M7 12v4")
        ),
    ":stethoscope" to listOf(
            LucidePathNode("M11 2v2"),
            LucidePathNode("M5 2v2"),
            LucidePathNode("M5 3H4a2 2 0 0 0-2 2v4a6 6 0 0 0 12 0V5a2 2 0 0 0-2-2h-1"),
            LucidePathNode("M8 15a6 6 0 0 0 12 0v-3"),
            LucideCircleNode(cx = 20f, cy = 10f, r = 2f)
        ),
    ":pill" to listOf(
            LucidePathNode("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"),
            LucidePathNode("m8.5 8.5 7 7")
        ),
    ":film" to listOf(
            LucideRectNode(x = 3f, y = 3f, width = 18f, height = 18f, rx = 2f, ry = 0f),
            LucidePathNode("M7 3v18"),
            LucidePathNode("M3 7.5h4"),
            LucidePathNode("M3 12h18"),
            LucidePathNode("M3 16.5h4"),
            LucidePathNode("M17 3v18"),
            LucidePathNode("M17 7.5h4"),
            LucidePathNode("M17 16.5h4")
        ),
    ":gamepad-2" to listOf(
            LucideLineNode(x1 = 6f, y1 = 11f, x2 = 10f, y2 = 11f),
            LucideLineNode(x1 = 8f, y1 = 9f, x2 = 8f, y2 = 13f),
            LucideLineNode(x1 = 15f, y1 = 12f, x2 = 15.01f, y2 = 12f),
            LucideLineNode(x1 = 18f, y1 = 10f, x2 = 18.01f, y2 = 10f),
            LucidePathNode("M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z")
        ),
    ":camera" to listOf(
            LucidePathNode("M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"),
            LucideCircleNode(cx = 12f, cy = 13f, r = 3f)
        ),
    ":users" to listOf(
            LucidePathNode("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"),
            LucideCircleNode(cx = 9f, cy = 7f, r = 4f),
            LucidePathNode("M22 21v-2a4 4 0 0 0-3-3.87"),
            LucidePathNode("M16 3.13a4 4 0 0 1 0 7.75")
        ),
    ":user-plus" to listOf(
            LucidePathNode("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"),
            LucideCircleNode(cx = 9f, cy = 7f, r = 4f),
            LucideLineNode(x1 = 19f, y1 = 8f, x2 = 19f, y2 = 14f),
            LucideLineNode(x1 = 22f, y1 = 11f, x2 = 16f, y2 = 11f)
        ),
    ":hand-heart" to listOf(
            LucidePathNode("M11 14h2a2 2 0 1 0 0-4h-3c-.6 0-1.1.2-1.4.6L3 16"),
            LucidePathNode("m7 20 1.6-1.4c.3-.4.8-.6 1.4-.6h4c1.1 0 2.1-.4 2.8-1.2l4.6-4.4a2 2 0 0 0-2.75-2.91l-4.2 3.9"),
            LucidePathNode("m2 15 6 6"),
            LucidePathNode("M19.5 8.5c.7-.7 1.5-1.6 1.5-2.7A2.73 2.73 0 0 0 16 4a2.78 2.78 0 0 0-5 1.8c0 1.2.8 2 1.5 2.8L16 12Z")
        ),
    ":file-text" to listOf(
            LucidePathNode("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"),
            LucidePathNode("M14 2v4a2 2 0 0 0 2 2h4"),
            LucidePathNode("M10 9H8"),
            LucidePathNode("M16 13H8"),
            LucidePathNode("M16 17H8")
        ),
    ":video" to listOf(
            LucidePathNode("m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5"),
            LucideRectNode(x = 2f, y = 6f, width = 14f, height = 12f, rx = 2f, ry = 0f)
        ),
    ":clipboard-check" to listOf(
            LucideRectNode(x = 8f, y = 2f, width = 8f, height = 4f, rx = 1f, ry = 1f),
            LucidePathNode("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"),
            LucidePathNode("m9 14 2 2 4-4")
        )
)

private val legacyHabitIconTokenMap = mapOf(
    "\uD83D\uDCA7" to ":droplet",
    "\uD83C\uDFC3" to ":footprints",
    "\uD83C\uDFCB" to ":dumbbell",
    "\uD83E\uDDD8" to ":brain",
    "\uD83D\uDCD6" to ":book-open",
    "\u270D" to ":pencil",
    "\uD83E\uDDF9" to ":home",
    "\uD83E\uDDB7" to ":sparkles",
    "\uD83E\uDEE5" to ":moon",
    "\uD83D\uDEB6" to ":footprints",
    "\uD83C\uDF7D" to ":apple",
    "\uD83D\uDC8A" to ":pill",
    "\uD83C\uDFB8" to ":music",
    "\uD83D\uDCBB" to ":code",
    ":water" to ":droplet",
    ":run" to ":footprints",
    ":workout" to ":dumbbell",
    ":meditate" to ":brain",
    ":read" to ":book-open",
    ":write" to ":pencil",
    ":clean" to ":home",
    ":brush" to ":sparkles",
    ":sleep" to ":moon",
    ":walk" to ":footprints",
    ":meal" to ":apple",
    ":medicine" to ":pill",
    ":music" to ":music",
    ":focus" to ":target"
)

fun resolveHabitIconToken(glyph: String): String? = when {
    glyph in habitIconNodes -> glyph
    glyph in legacyHabitIconTokenMap -> legacyHabitIconTokenMap.getValue(glyph)
    else -> null
}

fun isPresetHabitIcon(glyph: String): Boolean = resolveHabitIconToken(glyph) != null

internal fun habitIconNodesFor(token: String): List<LucideNode>? = habitIconNodes[token]
