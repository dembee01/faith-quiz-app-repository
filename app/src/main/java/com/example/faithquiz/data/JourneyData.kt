package com.example.faithquiz.data

data class JourneyNode(
    val level: Int,
    val title: String,
    val description: String,
    // Add positioning hints or asset references later
)

object JourneyData {
    val levels = listOf(
        JourneyNode(1, "The Beginning", "Creation & The Fall"),
        JourneyNode(2, "The Exodus", "Freedom from Egypt"),
        JourneyNode(3, "Kings & Prophets", "The Kingdom of Israel"),
        JourneyNode(4, "The Messiah", "Birth & Life of Jesus"),
        JourneyNode(5, "Divine Teacher", "Teachings of Christ"),
        JourneyNode(6, "The Gospel Spreads", "Acts & The Apostles"),
        JourneyNode(7, "Epistles of Paul", "Letters to the Churches"),
        JourneyNode(8, "Songs of Praise", "Psalms & Worship"),
        JourneyNode(9, "Wisdom's Voice", "Proverbs & Ecclesiastes"),
        JourneyNode(10, "Minor Prophets", "Messengers of God"),
        JourneyNode(11, "Major Prophets", "Visions of Glory"),
        JourneyNode(12, "Early Church", "Persecution & Growth"),
        JourneyNode(13, "Paul's Journeys", "Missionary Travels"),
        JourneyNode(14, "General Letters", "James, Peter, John"),
        JourneyNode(15, "Sacred Lands", "Biblical Geography"),
        JourneyNode(16, "History Unfolded", "Timeline of Events"),
        JourneyNode(17, "Miracles", "Signs & Wonders"),
        JourneyNode(18, "Parables", "Stories of the Kingdom"),
        JourneyNode(19, "Heroes of Faith", "Biblical Characters"),
        JourneyNode(20, "Sacred Numbers", "Symbolism in Math"),
        JourneyNode(21, "Biblical Symbols", "Types & Shadows"),
        JourneyNode(22, "Feasts & Festivals", "Holy Celebrations"),
        JourneyNode(23, "Covenants", "Promises of God"),
        JourneyNode(24, "Prophecy", "Future Foretold"),
        JourneyNode(25, "Theology 101", "Core Doctrines"),
        JourneyNode(26, "Original Languages", "Greek & Hebrew Roots"),
        JourneyNode(27, "Archaeology", "Digging Up Truth"),
        JourneyNode(28, "Interpretation", "Hermeneutics"),
        JourneyNode(29, "Deep Theology", "Advanced Mysteries"),
        JourneyNode(30, "The Covenant", "Biblical Mastery")
    )
}
