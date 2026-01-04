package com.example.faithquiz.data

import com.example.faithquiz.data.model.QuizQuestion

object QuestionBank {
    
    // Cache for questions to avoid recreating them on every call
    private val questionCache = mutableMapOf<Int, List<QuizQuestion>>()
    
    fun getQuestionsForLevel(level: Int): List<QuizQuestion> {
        // Return cached questions if available
        questionCache[level]?.let { return it }
        
        // Load and cache questions
        val questions = when (level) {
            1 -> level1Questions
            2 -> level2Questions
            3 -> level3Questions
            4 -> level4Questions
            5 -> level5Questions
            6 -> level6Questions
            7 -> level7Questions
            8 -> level8Questions
            9 -> level9Questions
            10 -> level10Questions
            11 -> level11Questions
            12 -> level12Questions
            13 -> level13Questions
            14 -> level14Questions
            15 -> level15Questions
            16 -> level16Questions
            17 -> level17Questions
            18 -> level18Questions
            19 -> level19Questions
            20 -> level20Questions
            21 -> level21Questions
            22 -> level22Questions
            23 -> level23Questions
            24 -> level24Questions
            25 -> level25Questions
            26 -> level26Questions
            27 -> level27Questions
            28 -> level28Questions
            29 -> level29Questions
            30 -> level30Questions
            else -> level1Questions // Fallback to level 1
        }
        
        // Validate questions and cache them
        val validatedQuestions = validateQuestions(questions, level)
        questionCache[level] = validatedQuestions
        return validatedQuestions
    }
    
    fun clearCache() {
        questionCache.clear()
    }
    
    fun getAvailableLevels(): List<Int> {
        return (1..30).toList()
    }
    
    private fun validateQuestions(questions: List<QuizQuestion>, level: Int): List<QuizQuestion> {
        if (questions.isEmpty()) {
            return level1Questions
        }
        
        val validQuestions = questions.filter { question ->
            isValidQuestion(question)
        }
        
        // Remove duplicates within a quiz by normalized question text
        val distinct = validQuestions.distinctBy { normalizeQuestionText(it.question) }
        
        return if (distinct.isNotEmpty()) distinct else level1Questions
    }

    private fun normalizeQuestionText(text: String): String {
        return text.lowercase().replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    }
    
    private fun isValidQuestion(question: QuizQuestion): Boolean {
        return try {
            // Validate question structure
            question.question.isNotBlank() &&
            question.options.size == 4 &&
            question.options.all { it.isNotBlank() } &&
            question.correctAnswer in 0..3 &&
            question.explanation.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
    
    // Level 1: Basic Bible Knowledge
    private val level1Questions = listOf(
        QuizQuestion(
            question = "Who was the first man according to the Bible?",
            options = listOf("Adam", "Noah", "Abraham", "Moses"),
            correctAnswer = 0,
            explanation = "Adam was the first man created by God in the Garden of Eden.",
            verseReference = "Genesis 2:7",
            verseText = "Then the LORD God formed the man of dust from the ground and breathed into his nostrils the breath of life, and the man became a living creature.",
            crossRefs = listOf("1 Corinthians 15:45")
        ),
        QuizQuestion(
            question = "How many days did it take God to create the world?",
            options = listOf("5 days", "6 days", "7 days", "8 days"),
            correctAnswer = 1,
            explanation = "God created the world in 6 days and rested on the 7th day.",
            verseReference = "Genesis 2:2",
            verseText = "And on the seventh day God finished his work that he had done, and he rested on the seventh day.",
            crossRefs = listOf("Exodus 20:11")
        ),
        QuizQuestion(
            question = "Who built the ark?",
            options = listOf("Moses", "David", "Noah", "Solomon"),
            correctAnswer = 2,
            explanation = "Noah built the ark to save his family and animals from the flood.",
            verseReference = "Genesis 6:14",
            verseText = "Make yourself an ark of gopher wood.",
            crossRefs = listOf("Hebrews 11:7")
        ),
        QuizQuestion(
            question = "What is the first book of the Bible?",
            options = listOf("Exodus", "Genesis", "Leviticus", "Psalms"),
            correctAnswer = 1,
            explanation = "Genesis is the first book of the Bible, meaning 'beginning'.",
            learnMoreUrl = "https://www.biblegateway.com/passage/?search=Genesis+1&version=ESV"
        ),
        QuizQuestion(
            question = "Who was swallowed by a great fish?",
            options = listOf("Jonah", "Job", "Joshua", "Jeremiah"),
            correctAnswer = 0,
            explanation = "Jonah was swallowed by a great fish when he tried to flee from God's command.",
            verseReference = "Jonah 1:17",
            verseText = "And the LORD appointed a great fish to swallow up Jonah.",
            crossRefs = listOf("Matthew 12:40")
        )
    )
    
    // Level 2: Old Testament Stories
    private val level2Questions = listOf(
        QuizQuestion(
            question = "Who led the Israelites out of Egypt?",
            options = listOf("Aaron", "Moses", "Joshua", "David"),
            correctAnswer = 1,
            explanation = "Moses led the Israelites out of slavery in Egypt with God's help."
        ),
        QuizQuestion(
            question = "How many plagues did God send to Egypt?",
            options = listOf("7", "10", "12", "15"),
            correctAnswer = 1,
            explanation = "God sent 10 plagues to Egypt to convince Pharaoh to let the Israelites go."
        ),
        QuizQuestion(
            question = "What did God give Moses on Mount Sinai?",
            options = listOf("The Ark", "The Ten Commandments", "A staff", "Food"),
            correctAnswer = 1,
            explanation = "God gave Moses the Ten Commandments written on stone tablets."
        ),
        QuizQuestion(
            question = "Who was Abraham's son of promise?",
            options = listOf("Ishmael", "Isaac", "Jacob", "Joseph"),
            correctAnswer = 1,
            explanation = "Isaac was the son God promised to Abraham and Sarah in their old age."
        ),
        QuizQuestion(
            question = "Which brother killed the other in the first murder?",
            options = listOf("Cain killed Abel", "Abel killed Cain", "Jacob killed Esau", "Esau killed Jacob"),
            correctAnswer = 0,
            explanation = "Cain killed his brother Abel out of jealousy over their offerings to God."
        )
    )
    
    // Level 3: Kings and Prophets
    private val level3Questions = listOf(
        QuizQuestion(
            question = "Who was the first king of Israel?",
            options = listOf("David", "Solomon", "Saul", "Samuel"),
            correctAnswer = 2,
            explanation = "Saul was the first king of Israel, chosen by the prophet Samuel."
        ),
        QuizQuestion(
            question = "Who killed the giant Goliath?",
            options = listOf("Saul", "Jonathan", "David", "Samuel"),
            correctAnswer = 2,
            explanation = "David, a young shepherd, killed Goliath with a sling and stone."
        ),
        QuizQuestion(
            question = "Who was known as the wisest king?",
            options = listOf("David", "Solomon", "Saul", "Hezekiah"),
            correctAnswer = 1,
            explanation = "Solomon was famous for his wisdom, which God gave him when he asked for it."
        ),
        QuizQuestion(
            question = "Which prophet was taken up to heaven in a whirlwind?",
            options = listOf("Elijah", "Elisha", "Isaiah", "Jeremiah"),
            correctAnswer = 0,
            explanation = "Elijah was taken up to heaven in a whirlwind with chariots of fire."
        ),
        QuizQuestion(
            question = "Who built the first temple in Jerusalem?",
            options = listOf("David", "Solomon", "Moses", "Aaron"),
            correctAnswer = 1,
            explanation = "Solomon built the first temple in Jerusalem as directed by his father David."
        )
    )
    
    // Level 4: New Testament Basics
    private val level4Questions = listOf(
        QuizQuestion(
            question = "Where was Jesus born?",
            options = listOf("Nazareth", "Jerusalem", "Bethlehem", "Capernaum"),
            correctAnswer = 2,
            explanation = "Jesus was born in Bethlehem, fulfilling Old Testament prophecy."
        ),
        QuizQuestion(
            question = "Who baptized Jesus?",
            options = listOf("Peter", "John the Baptist", "Andrew", "Philip"),
            correctAnswer = 1,
            explanation = "John the Baptist baptized Jesus in the Jordan River."
        ),
        QuizQuestion(
            question = "How many disciples did Jesus choose?",
            options = listOf("10", "12", "15", "20"),
            correctAnswer = 1,
            explanation = "Jesus chose 12 disciples to be his closest followers and apostles."
        ),
        QuizQuestion(
            question = "What was Jesus' first miracle?",
            options = listOf("Healing a blind man", "Walking on water", "Turning water to wine", "Feeding 5000"),
            correctAnswer = 2,
            explanation = "Jesus' first miracle was turning water into wine at a wedding in Cana."
        ),
        QuizQuestion(
            question = "Who denied Jesus three times?",
            options = listOf("Judas", "Peter", "John", "Thomas"),
            correctAnswer = 1,
            explanation = "Peter denied knowing Jesus three times before the rooster crowed, as Jesus predicted."
        )
    )
    
    // Level 5: Jesus' Teachings
    private val level5Questions = listOf(
        QuizQuestion(
            question = "What is the first beatitude?",
            options = listOf("Blessed are the meek", "Blessed are the poor in spirit", "Blessed are the peacemakers", "Blessed are the merciful"),
            correctAnswer = 1,
            explanation = "The first beatitude is 'Blessed are the poor in spirit, for theirs is the kingdom of heaven.'"
        ),
        QuizQuestion(
            question = "In the Lord's Prayer, what comes after 'Our Father who art in heaven'?",
            options = listOf("Give us this day", "Thy kingdom come", "Hallowed be thy name", "Forgive us our debts"),
            correctAnswer = 2,
            explanation = "The next line is 'Hallowed be thy name,' showing reverence for God's holiness."
        ),
        QuizQuestion(
            question = "What did Jesus say is the greatest commandment?",
            options = listOf("Do not steal", "Love God with all your heart", "Honor your parents", "Do not murder"),
            correctAnswer = 1,
            explanation = "Jesus said the greatest commandment is to love God with all your heart, soul, and mind."
        ),
        QuizQuestion(
            question = "Complete this saying: 'Do unto others...'",
            options = listOf("as they do unto you", "as you would have them do unto you", "better than yourself", "with kindness"),
            correctAnswer = 1,
            explanation = "The Golden Rule: 'Do unto others as you would have them do unto you.'"
        ),
        QuizQuestion(
            question = "What did Jesus say about worry?",
            options = listOf("Worry is wise", "Do not worry about tomorrow", "Worry helps us plan", "Worry shows care"),
            correctAnswer = 1,
            explanation = "Jesus taught not to worry about tomorrow, for each day has enough trouble of its own."
        )
    )
    
    // Continue with remaining levels (6-30) with progressively harder content
    private val level6Questions = listOf(
        QuizQuestion(
            question = "Who wrote most of the New Testament letters?",
            options = listOf("Peter", "John", "Paul", "James"),
            correctAnswer = 2,
            explanation = "The apostle Paul wrote 13 of the 27 New Testament books."
        ),
        QuizQuestion(
            question = "What was Paul's name before his conversion?",
            options = listOf("Saul", "Simon", "Samuel", "Stephen"),
            correctAnswer = 0,
            explanation = "Paul was originally named Saul and persecuted Christians before his conversion."
        ),
        QuizQuestion(
            question = "On which road did Paul meet Jesus?",
            options = listOf("Road to Jerusalem", "Road to Damascus", "Road to Rome", "Road to Antioch"),
            correctAnswer = 1,
            explanation = "Paul met the risen Jesus on the road to Damascus, leading to his conversion."
        ),
        QuizQuestion(
            question = "Who was the first Christian martyr?",
            options = listOf("James", "Stephen", "Peter", "John"),
            correctAnswer = 1,
            explanation = "Stephen was the first Christian martyr, stoned for his faith and testimony."
        ),
        QuizQuestion(
            question = "What does 'gospel' mean?",
            options = listOf("Good news", "God's word", "Great story", "Holy book"),
            correctAnswer = 0,
            explanation = "Gospel means 'good news' - the good news of salvation through Jesus Christ."
        )
    )
    
    // Adding more levels with increasing difficulty...
    private val level7Questions = listOf(
        QuizQuestion(
            question = "Which book comes after Acts in the New Testament?",
            options = listOf("Romans", "1 Corinthians", "Galatians", "Ephesians"),
            correctAnswer = 0,
            explanation = "Romans is the first letter of Paul that comes after the book of Acts."
        ),
        QuizQuestion(
            question = "How many books are in the New Testament?",
            options = listOf("25", "27", "29", "31"),
            correctAnswer = 1,
            explanation = "There are 27 books in the New Testament."
        ),
        QuizQuestion(
            question = "What does 'Christ' mean?",
            options = listOf("Savior", "Lord", "Anointed One", "King"),
            correctAnswer = 2,
            explanation = "'Christ' means 'Anointed One' or 'Messiah' in Greek."
        ),
        QuizQuestion(
            question = "Who wrote the book of Revelation?",
            options = listOf("Paul", "Peter", "John", "Luke"),
            correctAnswer = 2,
            explanation = "The apostle John wrote the book of Revelation while on the island of Patmos."
        ),
        QuizQuestion(
            question = "What are the first four books of the New Testament called?",
            options = listOf("Letters", "Gospels", "History", "Prophecy"),
            correctAnswer = 1,
            explanation = "Matthew, Mark, Luke, and John are called the Gospels."
        )
    )
    
    private val level8Questions = listOf(
        QuizQuestion(
            question = "How many books are in the Old Testament?",
            options = listOf("37", "39", "41", "43"),
            correctAnswer = 1,
            explanation = "There are 39 books in the Old Testament."
        ),
        QuizQuestion(
            question = "Which book is known as the 'book of praise'?",
            options = listOf("Psalms", "Proverbs", "Ecclesiastes", "Song of Songs"),
            correctAnswer = 0,
            explanation = "Psalms is known as the book of praise and worship."
        ),
        QuizQuestion(
            question = "Who wrote most of the Psalms?",
            options = listOf("Solomon", "David", "Moses", "Samuel"),
            correctAnswer = 1,
            explanation = "King David wrote about half of the 150 Psalms."
        ),
        QuizQuestion(
            question = "What does 'Hallelujah' mean?",
            options = listOf("God is good", "Praise the Lord", "Holy, holy", "Blessed be God"),
            correctAnswer = 1,
            explanation = "'Hallelujah' means 'Praise the Lord' or 'Praise Yahweh.'"
        ),
        QuizQuestion(
            question = "Which Psalm begins 'The Lord is my shepherd'?",
            options = listOf("Psalm 22", "Psalm 23", "Psalm 24", "Psalm 25"),
            correctAnswer = 1,
            explanation = "Psalm 23 is the famous shepherd psalm beginning 'The Lord is my shepherd.'"
        )
    )
    
    // For higher levels, provide richer, topic-specific question sets
    
    // Level 9: Wisdom Literature (Job, Psalms, Proverbs, Ecclesiastes, Song of Songs)
    private val level9Questions = listOf(
        QuizQuestion(
            question = "'The fear of the LORD is the beginning of wisdom' is found in which book?",
            options = listOf("Psalms", "Proverbs", "Ecclesiastes", "Job"),
            correctAnswer = 1,
            explanation = "Proverbs 9:10 states that the fear of the LORD is the beginning of wisdom."
        ),
        QuizQuestion(
            question = "Who is the central character who suffers greatly yet remains faithful?",
            options = listOf("David", "Job", "Solomon", "Asaph"),
            correctAnswer = 1,
            explanation = "Job is the central figure of the book of Job, enduring profound suffering."
        ),
        QuizQuestion(
            question = "Which book famously begins with 'Vanity of vanities, all is vanity'?",
            options = listOf("Psalms", "Proverbs", "Ecclesiastes", "Song of Songs"),
            correctAnswer = 2,
            explanation = "Ecclesiastes opens with this refrain, highlighting life's fleeting nature."
        ),
        QuizQuestion(
            question = "Which king is traditionally credited with writing many proverbs?",
            options = listOf("David", "Solomon", "Hezekiah", "Josiah"),
            correctAnswer = 1,
            explanation = "Solomon is credited with composing many of the proverbs."
        ),
        QuizQuestion(
            question = "In Job, which friend speaks last before the LORD answers out of the whirlwind?",
            options = listOf("Eliphaz", "Bildad", "Zophar", "Elihu"),
            correctAnswer = 3,
            explanation = "Elihu speaks in chapters 32–37 before God responds."
        ),
        QuizQuestion(
            question = "According to Proverbs, what is the beginning of knowledge?",
            options = listOf("Love", "Obedience", "The fear of the LORD", "Wisdom"),
            correctAnswer = 2,
            explanation = "Proverbs 1:7 states that the fear of the LORD is the beginning of knowledge.",
            verseReference = "Proverbs 1:7",
            verseText = "The fear of the LORD is the beginning of knowledge; fools despise wisdom and instruction."
        ),
        QuizQuestion(
            question = "According to Proverbs, what answer turns away wrath?",
            options = listOf("A gentle answer", "A loud rebuke", "Silence", "A sharp reply"),
            correctAnswer = 0,
            explanation = "Proverbs 15:1 says a gentle answer turns away wrath."
        ),
        QuizQuestion(
            question = "Which book celebrates marital love in poetic form?",
            options = listOf("Proverbs", "Song of Songs", "Ecclesiastes", "Job"),
            correctAnswer = 1,
            explanation = "Song of Songs (Song of Solomon) is a poetic celebration of love."
        ),
        QuizQuestion(
            question = "'Trust in the LORD with all your heart' appears in which chapter?",
            options = listOf("Proverbs 1", "Proverbs 3", "Proverbs 9", "Proverbs 31"),
            correctAnswer = 1,
            explanation = "Proverbs 3:5–6 encourages wholehearted trust in the LORD."
        ),
        QuizQuestion(
            question = "In Ecclesiastes, what time 'is there for everything'?",
            options = listOf("Harvest", "War", "A season", "Judgment"),
            correctAnswer = 2,
            explanation = "Ecclesiastes 3:1 says there is a season for every activity under the heavens."
        ),
        QuizQuestion(
            question = "Which Psalm is traditionally attributed to repentance after David's sin?",
            options = listOf("Psalm 32", "Psalm 40", "Psalm 51", "Psalm 103"),
            correctAnswer = 2,
            explanation = "Psalm 51 is David's prayer of repentance."
        ),
        QuizQuestion(
            question = "Who compiled additional proverbs of Solomon according to Proverbs 25:1?",
            options = listOf("Men of Hezekiah", "Scribes of Ezra", "Levites", "Sons of Korah"),
            correctAnswer = 0,
            explanation = "Proverbs 25:1 notes the men of Hezekiah copied additional proverbs of Solomon."
        ),
        QuizQuestion(
            question = "In Job, what does God ask Job about laying?",
            options = listOf("The foundations of the earth", "The stars in place", "The laws of physics", "The pillars of heaven"),
            correctAnswer = 0,
            explanation = "Job 38:4–7—God asks where Job was when He laid the earth's foundations."
        ),
        QuizQuestion(
            question = "Proverbs 31 describes a wife as being more precious than what?",
            options = listOf("Gold", "Rubies", "Silver", "Pearls"),
            correctAnswer = 1,
            explanation = "Proverbs 31:10 says more precious than rubies (or jewels)."
        ),
        QuizQuestion(
            question = "Which book repeatedly contrasts wisdom and folly through short sayings?",
            options = listOf("Ecclesiastes", "Proverbs", "Psalms", "Job"),
            correctAnswer = 1,
            explanation = "Proverbs is composed of concise sayings contrasting wisdom and folly."
        )
    )
    // Level 10: Minor Prophets (curated — replaces generic generator)
    private val level10Questions = listOf(
        QuizQuestion(
            question = "Which book is NOT one of the Minor Prophets?",
            options = listOf("Isaiah", "Hosea", "Amos", "Micah"),
            correctAnswer = 0,
            explanation = "Isaiah is a Major Prophet; Hosea, Amos, and Micah are Minor Prophets."
        ),
        QuizQuestion(
            question = "Which prophet was swallowed by a great fish?",
            options = listOf("Jonah", "Nahum", "Obadiah", "Micah"),
            correctAnswer = 0,
            explanation = "Jonah was swallowed by a great fish for three days and three nights (Jonah 1–2)."
        ),
        QuizQuestion(
            question = "Who married Gomer as a living parable of Israel's unfaithfulness?",
            options = listOf("Hosea", "Amos", "Malachi", "Zephaniah"),
            correctAnswer = 1,
            explanation = "God commanded Hosea to marry Gomer (Hosea 1)."
        ),
        QuizQuestion(
            question = "Who foretold that the Messiah would be born in Bethlehem?",
            options = listOf("Micah", "Haggai", "Zechariah", "Joel"),
            correctAnswer = 0,
            explanation = "Micah 5:2 prophesies Bethlehem as Messiah's birthplace."
        ),
        QuizQuestion(
            question = "Who declared, 'Not by might, nor by power, but by my Spirit'?",
            options = listOf("Zechariah", "Haggai", "Malachi", "Amos"),
            correctAnswer = 0,
            explanation = "Zechariah 4:6 emphasizes God's Spirit."
        ),
        QuizQuestion(
            question = "Who urged the returned exiles, 'Consider your ways', to rebuild the temple?",
            options = listOf("Haggai", "Zechariah", "Amos", "Joel"),
            correctAnswer = 0,
            explanation = "Haggai exhorted the people to rebuild (Haggai 1)."
        ),
        QuizQuestion(
            question = "Which prophet spoke of a locust plague as a picture of the Day of the LORD?",
            options = listOf("Joel", "Obadiah", "Nahum", "Zephaniah"),
            correctAnswer = 0,
            explanation = "Joel 1–2 uses the locust plague to call to repentance."
        ),
        QuizQuestion(
            question = "Which prophet focused on Nineveh's downfall?",
            options = listOf("Nahum", "Jonah", "Hosea", "Malachi"),
            correctAnswer = 0,
            explanation = "Nahum announces judgment on Nineveh."
        ),
        QuizQuestion(
            question = "Who wrote a single-chapter book condemning Edom?",
            options = listOf("Obadiah", "Haggai", "Joel", "Malachi"),
            correctAnswer = 0,
            explanation = "Obadiah rebukes Edom; it is one chapter."
        ),
        QuizQuestion(
            question = "Who proclaimed, 'The righteous shall live by faith'?",
            options = listOf("Habakkuk", "Zephaniah", "Amos", "Hosea"),
            correctAnswer = 0,
            explanation = "Habakkuk 2:4 is quoted in the New Testament."
        ),
        QuizQuestion(
            question = "Which prophet denounced social injustice in Israel's northern kingdom?",
            options = listOf("Amos", "Micah", "Joel", "Zechariah"),
            correctAnswer = 0,
            explanation = "Amos repeatedly condemns oppression and injustice."
        ),
        QuizQuestion(
            question = "Who warned, 'Will a man rob God?' concerning tithes and offerings?",
            options = listOf("Malachi", "Haggai", "Zechariah", "Obadiah"),
            correctAnswer = 0,
            explanation = "Malachi 3 addresses tithes and offerings."
        ),
        QuizQuestion(
            question = "Who called Israel to 'seek the LORD' and be hidden in the day of anger?",
            options = listOf("Zephaniah", "Nahum", "Joel", "Hosea"),
            correctAnswer = 0,
            explanation = "Zephaniah 2:3."
        ),
        QuizQuestion(
            question = "Which prophet was a shepherd from Tekoa?",
            options = listOf("Amos", "Micah", "Hosea", "Zechariah"),
            correctAnswer = 0,
            explanation = "Amos 1:1 records his background."
        ),
        QuizQuestion(
            question = "Who first fled toward Tarshish instead of obeying God's call?",
            options = listOf("Jonah", "Hosea", "Micah", "Zechariah"),
            correctAnswer = 0,
            explanation = "Jonah attempted to flee from God's presence (Jonah 1)."
        )
    )

    // Level 11: Major Prophets (curated — replaces generic generator)
    private val level11Questions = listOf(
        QuizQuestion(
            question = "Who saw the LORD 'high and lifted up' with seraphim crying 'Holy, holy, holy'?",
            options = listOf("Isaiah", "Jeremiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Isaiah's call vision appears in Isaiah 6."
        ),
        QuizQuestion(
            question = "Who was thrown into the lions' den?",
            options = listOf("Daniel", "Jeremiah", "Ezekiel", "Isaiah"),
            correctAnswer = 0,
            explanation = "Daniel 6 records the lions' den."
        ),
        QuizQuestion(
            question = "Which prophet promised a 'new covenant' written on the heart?",
            options = listOf("Jeremiah", "Ezekiel", "Isaiah", "Daniel"),
            correctAnswer = 0,
            explanation = "Jeremiah 31:31–34."
        ),
        QuizQuestion(
            question = "Who saw a valley of dry bones come to life?",
            options = listOf("Ezekiel", "Isaiah", "Jeremiah", "Daniel"),
            correctAnswer = 0,
            explanation = "Ezekiel 37 describes the vision."
        ),
        QuizQuestion(
            question = "Who interpreted the handwriting on the wall?",
            options = listOf("Daniel", "Ezekiel", "Jeremiah", "Isaiah"),
            correctAnswer = 0,
            explanation = "Daniel 5 explains 'Mene, Mene, Tekel, Parsin'."
        ),
        QuizQuestion(
            question = "Who is often called the 'weeping prophet'?",
            options = listOf("Jeremiah", "Isaiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Jeremiah lamented Judah's sin and suffering."
        ),
        QuizQuestion(
            question = "Which book mourns Jerusalem's fall in poetic laments?",
            options = listOf("Lamentations", "Daniel", "Ezekiel", "Isaiah"),
            correctAnswer = 0,
            explanation = "Lamentations is traditionally linked to Jeremiah."
        ),
        QuizQuestion(
            question = "Which prophet lay on his side and enacted signs of Jerusalem's siege?",
            options = listOf("Ezekiel", "Jeremiah", "Isaiah", "Daniel"),
            correctAnswer = 0,
            explanation = "Ezekiel performed symbolic actions (Ezekiel 4)."
        ),
        QuizQuestion(
            question = "Who prophesied a virgin bearing a son called Immanuel?",
            options = listOf("Isaiah", "Jeremiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Isaiah 7:14."
        ),
        QuizQuestion(
            question = "Which prophet served under Babylonian and Persian rulers?",
            options = listOf("Daniel", "Jeremiah", "Isaiah", "Ezekiel"),
            correctAnswer = 0,
            explanation = "Daniel served in the courts of several kings."
        ),
        QuizQuestion(
            question = "Who bought a field as a sign of future hope during Jerusalem's siege?",
            options = listOf("Jeremiah", "Isaiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Jeremiah 32 demonstrates hope in restoration."
        ),
        QuizQuestion(
            question = "Who saw 'wheels within wheels' and living creatures by the Chebar canal?",
            options = listOf("Ezekiel", "Isaiah", "Jeremiah", "Daniel"),
            correctAnswer = 0,
            explanation = "Ezekiel 1 describes this inaugural vision."
        ),
        QuizQuestion(
            question = "Which prophet includes 'Servant Songs' culminating in the Suffering Servant?",
            options = listOf("Isaiah", "Jeremiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Isaiah 42, 49, 50, 52–53 contain the Servant Songs."
        ),
        QuizQuestion(
            question = "In whose book do Shadrach, Meshach, and Abednego survive the fiery furnace?",
            options = listOf("Daniel", "Jeremiah", "Ezekiel", "Isaiah"),
            correctAnswer = 0,
            explanation = "Daniel 3 records the fiery furnace."
        ),
        QuizQuestion(
            question = "Who foretold a 'Branch' from Jesse's stump bringing righteous rule?",
            options = listOf("Isaiah", "Jeremiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "Isaiah 11 speaks of the Branch."
        )
    )
    private val level12Questions = createAdvancedQuestions(12, "Early Church", 15)
    private val level13Questions = createAdvancedQuestions(13, "Paul's Letters", 15)
    private val level14Questions = createAdvancedQuestions(14, "General Letters", 15)
    private val level15Questions = createAdvancedQuestions(15, "Biblical Geography", 15)
    private val level16Questions = createAdvancedQuestions(16, "Biblical History", 15)
    private val level17Questions = createAdvancedQuestions(17, "Miracles", 15)
    private val level18Questions = createAdvancedQuestions(18, "Parables", 15)
    private val level19Questions = createAdvancedQuestions(19, "Biblical Characters", 15)
    private val level20Questions = createAdvancedQuestions(20, "Biblical Numbers", 15)
    private val level21Questions = createAdvancedQuestions(21, "Biblical Symbols", 15)
    private val level22Questions = createAdvancedQuestions(22, "Biblical Festivals", 15)
    private val level23Questions = createAdvancedQuestions(23, "Biblical Covenants", 15)
    private val level24Questions = createAdvancedQuestions(24, "Biblical Prophecy", 15)
    private val level25Questions = createAdvancedQuestions(25, "Biblical Theology", 15)
    private val level26Questions = createAdvancedQuestions(26, "Biblical Languages", 15)
    private val level27Questions = createAdvancedQuestions(27, "Biblical Archaeology", 15)
    private val level28Questions = createAdvancedQuestions(28, "Biblical Interpretation", 15)
    private val level29Questions = createAdvancedQuestions(29, "Advanced Theology", 15)
    private val level30Questions = createAdvancedQuestions(30, "Biblical Mastery", 15)
    
    private fun createAdvancedQuestions(level: Int, topic: String, count: Int): List<QuizQuestion> {
        return when (topic) {
            "Early Church" -> getEarlyChurchQuestions(level)
            "Paul's Letters" -> getPaulsLettersQuestions(level)
            "General Letters" -> getGeneralLettersQuestions(level)
            "Biblical Geography" -> getBiblicalGeographyQuestions(level)
            "Biblical History" -> getBiblicalHistoryQuestions(level)
            "Miracles" -> getMiraclesQuestions(level)
            "Parables" -> getParablesQuestions(level)
            "Biblical Characters" -> getBiblicalCharactersQuestions(level)
            "Biblical Numbers" -> getBiblicalNumbersQuestions(level)
            "Biblical Symbols" -> getBiblicalSymbolsQuestions(level)
            "Biblical Festivals" -> getBiblicalFestivalsQuestions(level)
            "Biblical Covenants" -> getBiblicalCovenantsQuestions(level)
            "Biblical Prophecy" -> getBiblicalProphecyQuestions(level)
            "Biblical Theology" -> getBiblicalTheologyQuestions(level)
            "Biblical Languages" -> getBiblicalLanguagesQuestions(level)
            "Biblical Archaeology" -> getBiblicalArchaeologyQuestions(level)
            "Biblical Interpretation" -> getBiblicalInterpretationQuestions(level)
            "Advanced Theology" -> getAdvancedTheologyQuestions(level)
            "Biblical Mastery" -> getBiblicalMasteryQuestions(level)
            else -> getEarlyChurchQuestions(level)
        }.take(count)
    }
    
    // Early Church Questions (Level 12)
    private fun getEarlyChurchQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Who was chosen as one of the first seven deacons along with Stephen?",
                options = listOf("Philip", "Barnabas", "Silas", "Timothy"),
                correctAnswer = 0,
                explanation = "Philip was one of the seven chosen to serve tables in the early church.",
                verseReference = "Acts 6:5",
                verseText = "And they chose Stephen, a man full of faith... and Philip, and Prochorus..."
            ),
            QuizQuestion(
                question = "What happened on the Day of Pentecost?",
                options = listOf("Jesus ascended", "The Holy Spirit came", "Jesus was crucified", "The temple was destroyed"),
                correctAnswer = 1,
                explanation = "On the Day of Pentecost, the Holy Spirit descended upon the apostles.",
                verseReference = "Acts 2:1-4",
                verseText = "When the day of Pentecost arrived, they were all together in one place."
            ),
            QuizQuestion(
                question = "Who was the first Gentile convert?",
                options = listOf("Cornelius", "Lydia", "The Ethiopian eunuch", "Sergius Paulus"),
                correctAnswer = 0,
                explanation = "Cornelius was the first Gentile convert, baptized by Peter.",
                verseReference = "Acts 10:1-2",
                verseText = "At Caesarea there was a man named Cornelius, a centurion of what was known as the Italian Cohort."
            ),
            QuizQuestion(
                question = "Which apostle was known as 'the Rock'?",
                options = listOf("John", "Peter", "James", "Andrew"),
                correctAnswer = 1,
                explanation = "Peter was called 'the Rock' by Jesus, meaning 'stone' in Greek.",
                verseReference = "Matthew 16:18",
                verseText = "And I tell you, you are Peter, and on this rock I will build my church."
            ),
            QuizQuestion(
                question = "What was the Council of Jerusalem about?",
                options = listOf("Choosing apostles", "Circumcision of Gentiles", "Building churches", "Writing letters"),
                correctAnswer = 1,
                explanation = "The Council of Jerusalem decided that Gentile converts didn't need to be circumcised.",
                verseReference = "Acts 15:1-2",
                verseText = "But some men came down from Judea and were teaching the brothers, 'Unless you are circumcised according to the custom of Moses, you cannot be saved.'"
            ),
            QuizQuestion(
                question = "Which epistle is the longest letter Paul wrote?",
                options = listOf("Romans", "1 Corinthians", "Hebrews", "Ephesians"),
                correctAnswer = 0,
                explanation = "Romans is the longest and most systematically theological of Paul's letters.",
                verseReference = "Romans",
                verseText = "Paul, a servant of Christ Jesus..."
            ),
            QuizQuestion(
                question = "From which tribe was the Apostle Paul?",
                options = listOf("Judah", "Levi", "Benjamin", "Ephraim"),
                correctAnswer = 2,
                explanation = "Paul states he is of the tribe of Benjamin.",
                verseReference = "Philippians 3:5",
                verseText = "circumcised on the eighth day... of the tribe of Benjamin."
            ),
            QuizQuestion(
                question = "Which city was the center of early Christianity?",
                options = listOf("Rome", "Jerusalem", "Antioch", "Corinth"),
                correctAnswer = 1,
                explanation = "Jerusalem was the center of early Christianity, where the apostles first preached.",
                verseReference = "Acts 1:4",
                verseText = "And while staying with them he ordered them not to depart from Jerusalem."
            ),
            QuizQuestion(
                question = "Who was the first Gentile church?",
                options = listOf("Antioch", "Rome", "Corinth", "Ephesus"),
                correctAnswer = 0,
                explanation = "Antioch was the first major Gentile church and where believers were first called 'Christians'.",
                verseReference = "Acts 11:26",
                verseText = "And in Antioch the disciples were first called Christians."
            ),
            QuizQuestion(
                question = "What did the early Christians do daily?",
                options = listOf("Fast", "Pray", "Break bread together", "Travel"),
                correctAnswer = 2,
                explanation = "The early Christians met daily to break bread together and fellowship.",
                verseReference = "Acts 2:46",
                verseText = "And day by day, attending the temple together and breaking bread in their homes."
            ),
            QuizQuestion(
                question = "Who was the first Gentile to receive the Holy Spirit?",
                options = listOf("Cornelius", "Lydia", "The Ethiopian eunuch", "Sergius Paulus"),
                correctAnswer = 0,
                explanation = "Cornelius and his household were the first Gentiles to receive the Holy Spirit.",
                verseReference = "Acts 10:44-45",
                verseText = "While Peter was still saying these things, the Holy Spirit fell on all who heard the word."
            ),
            QuizQuestion(
                question = "Which apostle was killed by Herod?",
                options = listOf("Peter", "Paul", "James", "John"),
                correctAnswer = 2,
                explanation = "James, the brother of John, was killed by Herod Agrippa I.",
                verseReference = "Acts 12:2",
                verseText = "He killed James the brother of John with the sword."
            ),
            QuizQuestion(
                question = "What was the first miracle in the early church?",
                options = listOf("Healing the lame man", "Raising the dead", "Walking on water", "Feeding 5000"),
                correctAnswer = 0,
                explanation = "Peter and John healed a lame man at the Beautiful Gate of the temple.",
                verseReference = "Acts 3:6-8",
                verseText = "But Peter said, 'I have no silver and gold, but what I do have I give to you. In the name of Jesus Christ of Nazareth, rise up and walk!'"
            ),
            QuizQuestion(
                question = "Who was the first Gentile missionary?",
                options = listOf("Paul", "Barnabas", "Peter", "Philip"),
                correctAnswer = 0,
                explanation = "Paul was the first major missionary to the Gentiles, though Peter also ministered to them.",
                verseReference = "Acts 9:15",
                verseText = "But the Lord said to him, 'Go, for he is a chosen instrument of mine to carry my name before the Gentiles.'"
            ),
            QuizQuestion(
                question = "What was the first church council about?",
                options = listOf("Choosing deacons", "Gentile circumcision", "Church leadership", "Worship practices"),
                correctAnswer = 1,
                explanation = "The first church council in Jerusalem decided that Gentile converts didn't need circumcision.",
                verseReference = "Acts 15:6-7",
                verseText = "The apostles and the elders were gathered together to consider this matter."
            )
        )
    }
    
    // Paul's Letters Questions (Level 13)
    private fun getPaulsLettersQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which letter did Paul write from prison?",
                options = listOf("Romans", "Galatians", "Philippians", "1 Corinthians"),
                correctAnswer = 2,
                explanation = "Paul wrote Philippians, Colossians, Philemon, and Ephesians from prison.",
                verseReference = "Philippians 1:7",
                verseText = "It is right for me to feel this way about you all, because I hold you in my heart, for you are all partakers with me of grace, both in my imprisonment and in the defense and confirmation of the gospel."
            ),
            QuizQuestion(
                question = "What is the theme of Romans?",
                options = listOf("Love", "Justification by faith", "The church", "End times"),
                correctAnswer = 1,
                explanation = "Romans focuses on justification by faith and the righteousness of God.",
                verseReference = "Romans 1:17",
                verseText = "For in it the righteousness of God is revealed from faith for faith, as it is written, 'The righteous shall live by faith.'"
            ),
            QuizQuestion(
                question = "Which letter addresses the Galatian heresy?",
                options = listOf("Galatians", "Ephesians", "Philippians", "Colossians"),
                correctAnswer = 0,
                explanation = "Galatians was written to combat the Judaizers who taught that Gentiles must be circumcised.",
                verseReference = "Galatians 1:6-7",
                verseText = "I am astonished that you are so quickly deserting him who called you in the grace of Christ and are turning to a different gospel."
            ),
            QuizQuestion(
                question = "What does Paul call the 'love chapter'?",
                options = listOf("Romans 8", "1 Corinthians 13", "Ephesians 4", "Philippians 2"),
                correctAnswer = 1,
                explanation = "1 Corinthians 13 is known as the 'love chapter' describing the nature of love.",
                verseReference = "1 Corinthians 13:4",
                verseText = "Love is patient and kind; love does not envy or boast; it is not arrogant."
            ),
            QuizQuestion(
                question = "Which letter discusses the armor of God?",
                options = listOf("Ephesians", "Galatians", "Philippians", "Colossians"),
                correctAnswer = 0,
                explanation = "Ephesians 6 describes the full armor of God for spiritual warfare.",
                verseReference = "Ephesians 6:11",
                verseText = "Put on the whole armor of God, that you may be able to stand against the schemes of the devil."
            )
        )
    }
    
    // General Letters Questions (Level 14)
    private fun getGeneralLettersQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Who wrote the letter of James?",
                options = listOf("James, son of Zebedee", "James, brother of Jesus", "James, son of Alphaeus", "James the Less"),
                correctAnswer = 1,
                explanation = "James, the brother of Jesus, wrote the letter of James.",
                verseReference = "James 1:1",
                verseText = "James, a servant of God and of the Lord Jesus Christ, To the twelve tribes in the Dispersion."
            ),
            QuizQuestion(
                question = "What is the main theme of 1 Peter?",
                options = listOf("Love", "Suffering and hope", "Faith", "Wisdom"),
                correctAnswer = 1,
                explanation = "1 Peter focuses on suffering for Christ and the hope we have in Him.",
                verseReference = "1 Peter 1:3",
                verseText = "Blessed be the God and Father of our Lord Jesus Christ! According to his great mercy, he has caused us to be born again to a living hope through the resurrection of Jesus Christ from the dead."
            ),
            QuizQuestion(
                question = "Which letter warns against false teachers?",
                options = listOf("2 Peter", "Jude", "1 John", "All of the above"),
                correctAnswer = 3,
                explanation = "2 Peter, Jude, and 1 John all warn against false teachers and false doctrine.",
                verseReference = "2 Peter 2:1",
                verseText = "But false prophets also arose among the people, just as there will be false teachers among you."
            ),
            QuizQuestion(
                question = "What does John emphasize in his letters?",
                options = listOf("Love and truth", "Faith and works", "Hope and patience", "Wisdom and knowledge"),
                correctAnswer = 0,
                explanation = "John's letters emphasize love for one another and walking in truth.",
                verseReference = "1 John 4:7",
                verseText = "Beloved, let us love one another, for love is from God, and whoever loves has been born of God and knows God."
            ),
            QuizQuestion(
                question = "Which letter is about Christian maturity?",
                options = listOf("Hebrews", "James", "1 Peter", "1 John"),
                correctAnswer = 0,
                explanation = "Hebrews encourages believers to move from milk to solid food, from immaturity to maturity.",
                verseReference = "Hebrews 5:12-14",
                verseText = "For though by this time you ought to be teachers, you need someone to teach you again the basic principles of the oracles of God."
            )
        )
    }
    
    // Biblical Geography Questions (Level 15)
    private fun getBiblicalGeographyQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which city is known as the 'City of Palms'?",
                options = listOf("Jerusalem", "Jericho", "Samaria", "Bethany"),
                correctAnswer = 1,
                explanation = "Jericho is frequently called the City of Palms in Scripture.",
                verseReference = "Deuteronomy 34:3",
                verseText = "The Negeb... and the valley of Jericho the city of palm trees."
            ),
            QuizQuestion(
                question = "Which body of water is also known as the Lake of Gennesaret?",
                options = listOf("Dead Sea", "Mediterranean Sea", "Sea of Galilee", "Jordan River"),
                correctAnswer = 2,
                explanation = "The Sea of Galilee is also known as the Lake of Gennesaret and the Sea of Tiberias.",
                verseReference = "Luke 5:1",
                verseText = "On one occasion, while the crowd was pressing in on him to hear the word of God, he was standing by the lake of Gennesaret."
            ),
            QuizQuestion(
                question = "Where did Moses receive the Ten Commandments?",
                options = listOf("Mount Sinai", "Mount Zion", "Mount Carmel", "Mount Tabor"),
                correctAnswer = 0,
                explanation = "Moses received the Ten Commandments on Mount Sinai.",
                verseReference = "Exodus 19:20",
                verseText = "The LORD came down on Mount Sinai, to the top of the mountain."
            ),
            QuizQuestion(
                question = "Which city was known as the 'City of David'?",
                options = listOf("Jerusalem", "Bethlehem", "Hebron", "Nazareth"),
                correctAnswer = 0,
                explanation = "Jerusalem was known as the City of David after David captured it.",
                verseReference = "2 Samuel 5:7",
                verseText = "Nevertheless, David took the stronghold of Zion, that is, the city of David."
            ),
            QuizQuestion(
                question = "Where did the Israelites cross into the Promised Land?",
                options = listOf("Red Sea", "Jordan River", "Mediterranean Sea", "Dead Sea"),
                correctAnswer = 1,
                explanation = "The Israelites crossed the Jordan River to enter the Promised Land.",
                verseReference = "Joshua 3:17",
                verseText = "And the priests bearing the ark of the covenant of the LORD stood firmly on dry ground in the midst of the Jordan."
            )
        )
    }
    
    // Miracles Questions (Level 17)
    private fun getMiraclesQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Whose mother-in-law did Jesus heal of a fever?",
                options = listOf("Peter's", "James'", "John's", "Andrew's"),
                correctAnswer = 0,
                explanation = "Jesus healed Peter's mother-in-law at Peter's house.",
                verseReference = "Matthew 8:14-15",
                verseText = "And when Jesus entered Peter's house, he saw his mother-in-law lying sick with a fever."
            ),
            QuizQuestion(
                question = "How many people did Jesus feed with 5 loaves and 2 fish?",
                options = listOf("500", "1000", "5000", "10000"),
                correctAnswer = 2,
                explanation = "Jesus fed 5000 men (plus women and children) with 5 loaves and 2 fish.",
                verseReference = "Matthew 14:21",
                verseText = "And those who ate were about five thousand men, besides women and children."
            ),
            QuizQuestion(
                question = "Who did Jesus raise from the dead?",
                options = listOf("Lazarus only", "Jairus' daughter only", "The widow's son only", "All of the above"),
                correctAnswer = 3,
                explanation = "Jesus raised Lazarus, Jairus' daughter, and the widow's son from the dead.",
                verseReference = "John 11:43-44",
                verseText = "When he had said these things, he cried out with a loud voice, 'Lazarus, come out.'"
            ),
            QuizQuestion(
                question = "What happened when Jesus calmed the storm?",
                options = listOf("The wind stopped", "The waves became calm", "Both wind and waves stopped", "The boat was saved"),
                correctAnswer = 2,
                explanation = "When Jesus calmed the storm, both the wind and the waves became completely calm.",
                verseReference = "Mark 4:39",
                verseText = "And he awoke and rebuked the wind and said to the sea, 'Peace! Be still!' And the wind ceased, and there was a great calm."
            ),
            QuizQuestion(
                question = "How many lepers did Jesus heal at once?",
                options = listOf("One", "Two", "Ten", "Twelve"),
                correctAnswer = 2,
                explanation = "Jesus healed ten lepers at once, but only one returned to thank Him.",
                verseReference = "Luke 17:12-14",
                verseText = "And as he entered a village, he was met by ten lepers, who stood at a distance and lifted up their voices, saying, 'Jesus, Master, have mercy on us.'"
            )
        )
    }
    
    // Parables Questions (Level 18)
    private fun getParablesQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "In the Parable of the Sower, what does the seed represent?",
                options = listOf("The Sower", "The soil", "The Word of God", "The birds"),
                correctAnswer = 2,
                explanation = "Luke 8:11 states: 'The seed is the word of God.'",
                verseReference = "Luke 8:11",
                verseText = "Now the parable is this: The seed is the word of God."
            ),
            QuizQuestion(
                question = "In the Parable of the Good Samaritan, who passed by the injured man without helping?",
                options = listOf("A Soldier and a Merchant", "A Priest and a Levite", "A Pharisee and a Sadducee", "None of the above"),
                correctAnswer = 1,
                explanation = "A priest and a Levite both passed by on the other side before the Samaritan stopped to help.",
                verseReference = "Luke 10:31-32"
            ),
            QuizQuestion(
                question = "In the Parable of the Virgins, what did the five foolish virgins forget to bring?",
                options = listOf("Their lamps", "Oil for their lamps", "Wedding garments", "Their invitations"),
                correctAnswer = 1,
                explanation = "The foolish virgins took their lamps but took no oil with them (Matthew 25:3).",
                verseReference = "Matthew 25:3"
            ),
            QuizQuestion(
                question = "In the Parable of the Talents, what did the man with one talent do with it?",
                options = listOf("Invested it", "Lost it", "Buried it in the ground", "Gave it to charity"),
                correctAnswer = 2,
                explanation = "He was afraid and went and hid his talent in the ground.",
                verseReference = "Matthew 25:25"
            ),
            QuizQuestion(
                question = "In the Parable of the Lost Sheep, how many sheep did the shepherd leave behind to find the one?",
                options = listOf("9", "50", "99", "100"),
                correctAnswer = 2,
                explanation = "He leaves the ninety-nine in the open country to go after the one that is lost.",
                verseReference = "Luke 15:4"
            )
        )
    }
    
    // Placeholder functions for other topics (to be implemented)
    // Biblical History (Level 16)
    private fun getBiblicalHistoryQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which empire destroyed the First Temple in 586 BC?",
                options = listOf("Assyrian", "Babylonian", "Persian", "Roman"),
                correctAnswer = 1,
                explanation = "The Babylonian Empire, led by Nebuchadnezzar, destroyed Jerusalem and the Temple.",
                verseReference = "2 Kings 25:8-9"
            ),
            QuizQuestion(
                question = "Who issued the decree allowing Jews to return and rebuild the Temple?",
                options = listOf("Cyrus the Great", "Darius", "Artaxerxes", "Nebuchadnezzar"),
                correctAnswer = 0,
                explanation = "Cyrus the Great of Persia issued the decree fulfilling Jeremiah's prophecy.",
                verseReference = "Ezra 1:1-2"
            ),
            QuizQuestion(
                question = "Which Roman governor sentenced Jesus to crucifixion?",
                options = listOf("Herod Antipas", "Pontius Pilate", "Felix", "Festus"),
                correctAnswer = 1,
                explanation = "Pontius Pilate was the Roman governor of Judea who sentenced Jesus.",
                verseReference = "Matthew 27:26"
            ),
            QuizQuestion(
                question = "Which event marks the beginning of the Church age?",
                options = listOf("The Resurrection", "The Ascension", "Pentecost", "The Council of Jerusalem"),
                correctAnswer = 2,
                explanation = "Pentecost, the coming of the Holy Spirit, marks the birth of the Church.",
                verseReference = "Acts 2"
            ),
            QuizQuestion(
                question = "Who was the High Priest during Jesus' trial?",
                options = listOf("Annas", "Caiaphas", "Gamaliel", "Nicodemus"),
                correctAnswer = 1,
                explanation = "Caiaphas was the High Priest who prophesied that one man should die for the people.",
                verseReference = "John 11:49-50"
            )
        )
    }
    // Biblical Characters (Level 19)
    private fun getBiblicalCharactersQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Who climbed a sycamore tree to see Jesus?",
                options = listOf("Nicodemus", "Zacchaeus", "Bartimaeus", "Lazarus"),
                correctAnswer = 1,
                explanation = "Zacchaeus, a tax collector, climbed a tree because he was short.",
                verseReference = "Luke 19:4"
            ),
            QuizQuestion(
                question = "Who sold his birthright for a bowl of stew?",
                options = listOf("Jacob", "Esau", "Joseph", "Reuben"),
                correctAnswer = 1,
                explanation = "Esau despised his birthright and sold it to Jacob for food.",
                verseReference = "Genesis 25:33"
            ),
            QuizQuestion(
                question = "Who was the great-grandmother of King David?",
                options = listOf("Rahab", "Ruth", "Naomi", "Hannah"),
                correctAnswer = 1,
                explanation = "Ruth the Moabitess was the great-grandmother of King David.",
                verseReference = "Ruth 4:17"
            ),
            QuizQuestion(
                question = "Which judge made a vow that cost him his daughter?",
                options = listOf("Gideon", "Samson", "Jephthah", "Barak"),
                correctAnswer = 2,
                explanation = "Jephthah made a rash vow to sacrifice whatever came out of his house.",
                verseReference = "Judges 11:30-31"
            ),
            QuizQuestion(
                question = "Who did God speak to from a burning bush?",
                options = listOf("Abraham", "Moses", "Joshua", "Elijah"),
                correctAnswer = 1,
                explanation = "God spoke to Moses from the burning bush at Horeb.",
                verseReference = "Exodus 3:2"
            )
        )
    }
    // Biblical Numbers (Level 20) - Hard Mode
    private fun getBiblicalNumbersQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Exactly how many fish were caught in the miraculous catch recorded in John 21?",
                options = listOf("120", "144", "153", "1000"),
                correctAnswer = 2,
                explanation = "Simon Peter hauled the net ashore, full of large fish, 153 of them. (John 21:11)",
                verseReference = "John 21:11"
            ),
            QuizQuestion(
                question = "How old was Methuselah when he died?",
                options = listOf("900", "930", "950", "969"),
                correctAnswer = 3,
                explanation = "Methuselah lived 969 years, the longest human lifespan recorded in the Bible.",
                verseReference = "Genesis 5:27"
            ),
            QuizQuestion(
                question = "How many years did the invalid man wait by the pool of Bethesda?",
                options = listOf("12 years", "38 years", "40 years", "50 years"),
                correctAnswer = 1,
                explanation = "One man was there who had been an invalid for thirty-eight years. (John 5:5)",
                verseReference = "John 5:5"
            ),
            QuizQuestion(
                question = "According to 1 Kings, how many proverbs did Solomon speak?",
                options = listOf("1000", "3000", "1005", "5000"),
                correctAnswer = 1,
                explanation = "He also spoke 3,000 proverbs, and his songs were 1,005. (1 Kings 4:32)",
                verseReference = "1 Kings 4:32"
            ),
            QuizQuestion(
                question = "How many men of war did Gideon finally take into battle against Midian?",
                options = listOf("300", "10,000", "32,000", "500"),
                correctAnswer = 0,
                explanation = "The LORD said to Gideon, 'With the 300 men who lapped I will save you.' (Judges 7:7)",
                verseReference = "Judges 7:7"
            )
        )
    }
    // Biblical Symbols (Level 21)
    private fun getBiblicalSymbolsQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "What does the 'Lamb of God' represent?",
                options = listOf("Innocence", "Jesus as the sacrifice", "The nation of Israel", "The Church"),
                correctAnswer = 1,
                explanation = "The Lamb represents Jesus, the perfect sacrifice who takes away the sin of the world.",
                verseReference = "John 1:29"
            ),
            QuizQuestion(
                question = "In the armor of God, what does the 'Sword of the Spirit' represent?",
                options = listOf("Prayer", "Faith", "The Word of God", "Salvation"),
                correctAnswer = 2,
                explanation = "The Sword of the Spirit is the Word of God.",
                verseReference = "Ephesians 6:17"
            ),
            QuizQuestion(
                question = "What did the veil tearing in the temple symbolize?",
                options = listOf("God's anger", "Access to God", "The end of the world", "The destruction of the temple"),
                correctAnswer = 1,
                explanation = "It symbolized that through Jesus' death, we have direct access to God.",
                verseReference = "Matthew 27:51"
            ),
            QuizQuestion(
                question = "What does 'leaven' (yeast) often symbolize in the Bible?",
                options = listOf("Growth", "Sin/Corruption", "Joy", "Provision"),
                correctAnswer = 1,
                explanation = "Leaven often symbolizes sin or false teaching that spreads and puffs up.",
                verseReference = "1 Corinthians 5:6"
            ),
            QuizQuestion(
                question = "What does the rainbow symbolize?",
                options = listOf("God's covenant with Noah", "The Holy Spirit", "Creation", "Peace"),
                correctAnswer = 0,
                explanation = "The rainbow is the sign of God's covenant never to destroy the earth by flood again.",
                verseReference = "Genesis 9:13"
            )
        )
    }
    // Biblical Festivals (Level 22)
    private fun getBiblicalFestivalsQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which festival celebrates the Israelites' deliverance from Egypt?",
                options = listOf("Pentecost", "Passover", "Tabernacles", "Purim"),
                correctAnswer = 1,
                explanation = "Passover commemorates the night the angel of death passed over Israelite homes.",
                verseReference = "Exodus 12:27"
            ),
            QuizQuestion(
                question = "What is the Feast of Weeks also known as?",
                options = listOf("Passover", "Pentecost", "Trumpets", "Atonement"),
                correctAnswer = 1,
                explanation = "The Feast of Weeks is also known as Pentecost (50 days after Passover).",
                verseReference = "Leviticus 23:15-16"
            ),
            QuizQuestion(
                question = "Which festival involved living in booths or tents?",
                options = listOf("Passover", "Pentecost", "Tabernacles", "Dedication"),
                correctAnswer = 2,
                explanation = "The Feast of Tabernacles (Booths) reminded them of their wilderness journey.",
                verseReference = "Leviticus 23:42"
            ),
            QuizQuestion(
                question = "What is the Day of Atonement called in Hebrew?",
                options = listOf("Rosh Hashanah", "Yom Kippur", "Hanukkah", "Purim"),
                correctAnswer = 1,
                explanation = "Yom Kippur is the Day of Atonement, the holiest day of the year.",
                verseReference = "Leviticus 23:27"
            ),
            QuizQuestion(
                question = "Which festival celebrates the preservation of the Jews from Haman's plot?",
                options = listOf("Passover", "Purim", "Hanukkah", "Pentecost"),
                correctAnswer = 1,
                explanation = "Purim celebrates the events recorded in the book of Esther.",
                verseReference = "Esther 9:26"
            )
        )
    }
    // Biblical Covenants (Level 23)
    private fun getBiblicalCovenantsQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which covenant is associated with the rainbow?",
                options = listOf("Abrahamic", "Noahic", "Mosaic", "Davidic"),
                correctAnswer = 1,
                explanation = "The Noahic Covenant promised that God would never flood the whole earth again.",
                verseReference = "Genesis 9:13"
            ),
            QuizQuestion(
                question = "What was the sign of the Abrahamic Covenant?",
                options = listOf("Sabbath", "Circumcision", "Rainbow", "Passover"),
                correctAnswer = 1,
                explanation = "Circumcision was the sign of God's covenant with Abraham.",
                verseReference = "Genesis 17:11"
            ),
            QuizQuestion(
                question = "Which covenant promised a descendant on the throne forever?",
                options = listOf("Mosaic", "Davidic", "New", "Abrahamic"),
                correctAnswer = 1,
                explanation = "The Davidic Covenant promised that David's throne would be established forever.",
                verseReference = "2 Samuel 7:16"
            ),
            QuizQuestion(
                question = "Who is the mediator of the New Covenant?",
                options = listOf("Moses", "Abraham", "David", "Jesus"),
                correctAnswer = 3,
                explanation = "Jesus is the mediator of the New Covenant, sealed by His blood.",
                verseReference = "Hebrews 9:15"
            ),
            QuizQuestion(
                question = "Where was the Mosaic Covenant established?",
                options = listOf("Mount Zion", "Mount Sinai", "Mount Ararat", "Mount Carmel"),
                correctAnswer = 1,
                explanation = "The Mosaic Covenant (the Law) was given at Mount Sinai.",
                verseReference = "Exodus 19"
            )
        )
    }
    // Biblical Prophecy (Level 24) - Hard Mode
    private fun getBiblicalProphecyQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Which prophet was told to buy a linen loincloth and hide it by the Euphrates?",
                options = listOf("Ezekiel", "Jeremiah", "Isaiah", "Hosea"),
                correctAnswer = 1,
                explanation = "Jeremiah 13:4 - 'Take the loincloth... and go to the Euphrates and hide it there in a cleft of the rock.'",
                verseReference = "Jeremiah 13:4"
            ),
            QuizQuestion(
                question = "Who prophesied about the 'Valley of Decision'?",
                options = listOf("Zechariah", "Joel", "Amos", "Obadiah"),
                correctAnswer = 1,
                explanation = "Multitudes, multitudes, in the valley of decision! For the day of the LORD is near... (Joel 3:14)",
                verseReference = "Joel 3:14"
            ),
            QuizQuestion(
                question = "In Daniel's vision of the four beasts, what did the third beast like a leopard have on its back?",
                options = listOf("Two horns", "Four wings of a bird", "Iron teeth", "Ten horns"),
                correctAnswer = 1,
                explanation = "The third beast was like a leopard, with four wings of a bird on its back and four heads. (Daniel 7:6)",
                verseReference = "Daniel 7:6"
            ),
            QuizQuestion(
                question = "Which prophet explicitly addresses Zerubbabel the governor and Joshua the high priest?",
                options = listOf("Malachi", "Haggai", "Jonah", "Nahum"),
                correctAnswer = 1,
                explanation = "Haggai spoke the word of the LORD to Zerubbabel... and to Joshua... (Haggai 1:1)",
                verseReference = "Haggai 1:1"
            ),
            QuizQuestion(
                question = "Who prophesied that the Messiah would be 'cut off' but not for himself?",
                options = listOf("Isaiah", "Daniel", "Jeremiah", "Ezekiel"),
                correctAnswer = 1,
                explanation = "Daniel 9:26 states 'And after the sixty-two weeks, an anointed one shall be cut off and shall have nothing.'",
                verseReference = "Daniel 9:26"
            )
        )
    }
    // Biblical Theology (Level 25) - Hard Mode
    private fun getBiblicalTheologyQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "What does the theological term 'Kenosis' refer to?",
                options = listOf("Christ's resurrection", "Christ's self-emptying", "Christ's second coming", "Christ's ascension"),
                correctAnswer = 1,
                explanation = "Kenosis refers to Christ 'emptying himself' by taking the form of a servant (Philippians 2:7).",
                verseReference = "Philippians 2:7"
            ),
            QuizQuestion(
                question = "What Greek term is used in 2 Timothy 3:16 to describe Scripture?",
                options = listOf("Theopneustos", "Logos", "Rhema", "Graphe"),
                correctAnswer = 0,
                explanation = "Theopneustos means 'God-breathed' or 'inspired by God'.",
                verseReference = "2 Timothy 3:16"
            ),
            QuizQuestion(
                question = "What is 'Propitiation'?",
                options = listOf("Cleansing from sin", "Turning away God's wrath", "Adoption as sons", "Sanctification"),
                correctAnswer = 1,
                explanation = "Propitiation (Hilasterion) means satisfying or turning away God's wrath through a sacrifice (Romans 3:25).",
                verseReference = "Romans 3:25"
            ),
            QuizQuestion(
                question = "In the Council of Chalcedon, what was defined regarding Christ?",
                options = listOf("He is two persons", "He has one nature", "He is one person with two natures", "He is a created being"),
                correctAnswer = 2,
                explanation = "Chalcedon defined Christ as one person with two distinct natures (divine and human), unconfused and undivided.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "What does 'Imputation' mean in the context of justification?",
                options = listOf("Infusing righteousness", "Crediting righteousness to an account", "Erasing sin", "Making someone perfect"),
                correctAnswer = 1,
                explanation = "Imputation means crediting Christ's righteousness to the believer's account (Romans 4:6-8).",
                verseReference = "Romans 4:6-8"
            )
        )
    }
    // Biblical Languages (Level 26)
    private fun getBiblicalLanguagesQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "What language was the Old Testament primarily written in?",
                options = listOf("Greek", "Hebrew", "Latin", "Aramaic"),
                correctAnswer = 1,
                explanation = "The Old Testament was primarily written in Hebrew, with some portions in Aramaic.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "What language was the New Testament written in?",
                options = listOf("Hebrew", "Greek", "Latin", "Syriac"),
                correctAnswer = 1,
                explanation = "The New Testament was written in Koine (Common) Greek.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "What does the Hebrew word 'Shalom' mean?",
                options = listOf("God", "Peace", "Love", "Victory"),
                correctAnswer = 1,
                explanation = "Shalom means peace, wholeness, and well-being.",
                verseReference = "Judges 6:24"
            ),
            QuizQuestion(
                question = "What does 'Logos' mean in John 1?",
                options = listOf("Light", "Word", "Life", "Truth"),
                correctAnswer = 1,
                explanation = "Logos translates to 'Word' in John 1:1 ('In the beginning was the Word').",
                verseReference = "John 1:1"
            ),
            QuizQuestion(
                question = "What does 'Abba' mean?",
                options = listOf("Master", "Father", "King", "Teacher"),
                correctAnswer = 1,
                explanation = "Abba is an Aramaic term for Father, denoting intimacy.",
                verseReference = "Galatians 4:6"
            )
        )
    }
    // Biblical Archaeology (Level 27) - Hard Mode
    private fun getBiblicalArchaeologyQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "The 'Black Obelisk of Shalmaneser III' depicts which Israelite king bowing down?",
                options = listOf("Ahab", "Jehu", "Omri", "Jeroboam II"),
                correctAnswer = 1,
                explanation = "It depicts Jehu paying tribute to the Assyrian king, the only known image of an Israelite king.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "The 'Ketef Hinnom Silver Scrolls' contain the oldest known text of what?",
                options = listOf("The Ten Commandments", "The Shema", "The Priestly Blessing", "Psalm 23"),
                correctAnswer = 2,
                explanation = "They contain the Priestly Blessing from Numbers 6:24-26, dating to c. 600 BC.",
                verseReference = "Numbers 6:24-26"
            ),
            QuizQuestion(
                question = "Which Assyrian king's prism boasts, 'Hezekiah the Judahite I shut up... like a bird in a cage'?",
                options = listOf("Sargon II", "Sennacherib", "Tiglath-Pileser III", "Esarhaddon"),
                correctAnswer = 1,
                explanation = "Sennacherib's Prism details his siege of Jerusalem in 701 BC.",
                verseReference = "2 Kings 18-19"
            ),
            QuizQuestion(
                question = "The 'Moabite Stone' (Mesha Stele) mentions which King of Israel whom Mesha rebelled against?",
                options = listOf("Omri", "Ahab", "Jehu", "Joram"),
                correctAnswer = 0,
                explanation = "It explicitly mentions Omri, king of Israel, having oppressed Moab.",
                verseReference = "2 Kings 3:4"
            ),
            QuizQuestion(
                question = "The 'Pilate Stone' discovered at Caesarea Maritima confirms Pilate held what title?",
                options = listOf("Procurator", "Prefect", "Governor", "Tetrarch"),
                correctAnswer = 1,
                explanation = "The inscription identifies him as 'Prefect of Judea' (Praefectus Iudaeae).",
                verseReference = "N/A"
            )
        )
    }
    // Biblical Interpretation (Level 28)
    private fun getBiblicalInterpretationQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "What is a 'Parable'?",
                options = listOf("A historical fact", "An earthly story with a heavenly meaning", "A poem", "A proverb"),
                correctAnswer = 1,
                explanation = "A parable is often described as an earthly story with a heavenly meaning.",
                verseReference = "Matthew 13"
            ),
            QuizQuestion(
                question = "What is 'Exegesis'?",
                options = listOf("Reading into the text", "Drawing meaning out of the text", "Ignoring the text", "Translating the text"),
                correctAnswer = 1,
                explanation = "Exegesis means drawing the meaning out of the text (as opposed to Eisegesis).",
                verseReference = "2 Timothy 2:15"
            ),
            QuizQuestion(
                question = "Which literary genre is Psalms?",
                options = listOf("Prophecy", "History", "Poetry", "Law"),
                correctAnswer = 2,
                explanation = "Psalms is a book of Hebrew poetry and songs.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "What is the 'Pentateuch'?",
                options = listOf("The New Testament", "The Prophets", "The first five books of Moses", "The Wisdom books"),
                correctAnswer = 2,
                explanation = "The Pentateuch (Torah) refers to the first five books of the Bible.",
                verseReference = "N/A"
            ),
            QuizQuestion(
                question = "In prophetic literature, what does 'Day of the LORD' usually signify?",
                options = listOf("Sunday", "A day of judgment/salvation", "The Sabbath", "Creation"),
                correctAnswer = 1,
                explanation = "The Day of the LORD refers to a time of God's decisive intervention for judgment or salvation.",
                verseReference = "Joel 2:1"
            )
        )
    }
    // Advanced Theology (Level 29)
    private fun getAdvancedTheologyQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "What is 'Sola Fide'?",
                options = listOf("Scripture Alone", "Faith Alone", "Grace Alone", "Christ Alone"),
                correctAnswer = 1,
                explanation = "Sola Fide is the Reformation doctrine of Justification by Faith Alone.",
                verseReference = "Romans 3:28"
            ),
            QuizQuestion(
                question = "What does 'Omnipresent' mean?",
                options = listOf("All-knowing", "All-powerful", "Present everywhere", "Unchanging"),
                correctAnswer = 2,
                explanation = "Omnipresent means God is present everywhere at all times.",
                verseReference = "Psalm 139:7-10"
            ),
            QuizQuestion(
                question = "What is the 'Hypostatic Union'?",
                options = listOf("Church unity", "Jesus as 100% God and 100% Man", "Marriage", "The Trinity"),
                correctAnswer = 1,
                explanation = "Hypostatic Union describes the union of Christ's divine and human natures in one person.",
                verseReference = "Philippians 2:6-7"
            ),
            QuizQuestion(
                question = "What is 'Eschatology' the study of?",
                options = listOf("Sin", "Salvation", "The End Times", "The Church"),
                correctAnswer = 2,
                explanation = "Eschatology is the theology concerned with death, judgment, and the final destiny of the soul and humankind.",
                verseReference = "Revelation 21"
            ),
            QuizQuestion(
                question = "What refers to God's all-knowing nature?",
                options = listOf("Omnipotence", "Omniscience", "Omnipresence", "Immutability"),
                correctAnswer = 1,
                explanation = "Omniscience means God knows everything.",
                verseReference = "1 John 3:20"
            )
        )
    }
    // Biblical Mastery (Level 30) - Very Hard
    private fun getBiblicalMasteryQuestions(level: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "Who fell out of a third-story window while Paul was preaching until midnight?",
                options = listOf("Tychicus", "Eutychus", "Archippus", "Epaphras"),
                correctAnswer = 1,
                explanation = "Eutychus, a young man, sank into a deep sleep and fell from the third story. (Acts 20:9)",
                verseReference = "Acts 20:9"
            ),
            QuizQuestion(
                question = "Who killed the obesity-stricken King Eglon of Moab?",
                options = listOf("Shamgar", "Barak", "Ehud", "Othniel"),
                correctAnswer = 2,
                explanation = "Ehud the son of Gera, a left-handed man, killed Eglon the king of Moab. (Judges 3:15-22)",
                verseReference = "Judges 3:15"
            ),
            QuizQuestion(
                question = "What was the name of the bronze serpent Moses made, which the Israelites later worshipped?",
                options = listOf("Leviathan", "Nehushtan", "Rahab", "Behemoth"),
                correctAnswer = 1,
                explanation = "Hezekiah broke in pieces the bronze serpent, for the people called it Nehushtan. (2 Kings 18:4)",
                verseReference = "2 Kings 18:4"
            ),
            QuizQuestion(
                question = "Who was the father of the prophet Isaiah?",
                options = listOf("Amos", "Amoz", "Hilkiah", "Beeri"),
                correctAnswer = 1,
                explanation = "The vision of Isaiah the son of Amoz. (Isaiah 1:1) Note: Amoz is distinct from the prophet Amos.",
                verseReference = "Isaiah 1:1"
            ),
            QuizQuestion(
                question = "Which chapter of the Bible is the longest?",
                options = listOf("Psalm 119", "Psalm 117", "Numbers 7", "Matthew 26"),
                correctAnswer = 0,
                explanation = "Psalm 119 is the longest chapter in the Bible with 176 verses.",
                verseReference = "N/A"
            )
        )
    }
}
