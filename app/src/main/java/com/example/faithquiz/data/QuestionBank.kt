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
            question = "Which Psalm begins 'The LORD is my shepherd'?",
            options = listOf("Psalm 1", "Psalm 19", "Psalm 23", "Psalm 51"),
            correctAnswer = 2,
            explanation = "Psalm 23 is the shepherd psalm."
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
                question = "Who was the first Christian martyr?",
                options = listOf("Stephen", "James", "Peter", "Paul"),
                correctAnswer = 0,
                explanation = "Stephen was the first Christian martyr, stoned to death for his faith.",
                verseReference = "Acts 7:59",
                verseText = "And as they were stoning Stephen, he called out, 'Lord Jesus, receive my spirit.'"
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
                question = "Who wrote most of the New Testament letters?",
                options = listOf("Peter", "John", "Paul", "James"),
                correctAnswer = 2,
                explanation = "Paul wrote 13 of the 27 New Testament books.",
                verseReference = "Various",
                verseText = "Paul's letters make up a significant portion of the New Testament."
            ),
            QuizQuestion(
                question = "What was Paul's original name?",
                options = listOf("Saul", "Simon", "Stephen", "Silas"),
                correctAnswer = 0,
                explanation = "Paul was originally named Saul before his conversion on the road to Damascus.",
                verseReference = "Acts 9:1",
                verseText = "But Saul, still breathing threats and murder against the disciples of the Lord."
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
                question = "Where was Jesus born?",
                options = listOf("Nazareth", "Bethlehem", "Jerusalem", "Capernaum"),
                correctAnswer = 1,
                explanation = "Jesus was born in Bethlehem, the city of David.",
                verseReference = "Luke 2:4-5",
                verseText = "And Joseph also went up from Galilee, from the town of Nazareth, to Judea, to the city of David, which is called Bethlehem."
            ),
            QuizQuestion(
                question = "Which sea did Jesus walk on?",
                options = listOf("Mediterranean Sea", "Red Sea", "Sea of Galilee", "Dead Sea"),
                correctAnswer = 2,
                explanation = "Jesus walked on the Sea of Galilee, also called the Sea of Tiberias.",
                verseReference = "Matthew 14:25",
                verseText = "And in the fourth watch of the night he came to them, walking on the sea."
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
                question = "What was Jesus' first miracle?",
                options = listOf("Healing the blind", "Walking on water", "Turning water to wine", "Raising Lazarus"),
                correctAnswer = 2,
                explanation = "Jesus' first miracle was turning water into wine at the wedding in Cana.",
                verseReference = "John 2:11",
                verseText = "This, the first of his signs, Jesus did at Cana in Galilee, and manifested his glory."
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
                question = "What is the Parable of the Sower about?",
                options = listOf("Farming", "Different types of soil", "The Word of God", "All of the above"),
                correctAnswer = 3,
                explanation = "The Parable of the Sower uses farming and different soil types to teach about how people receive the Word of God.",
                verseReference = "Matthew 13:3-9",
                verseText = "A sower went out to sow. And as he sowed, some seeds fell along the path, and the birds came and devoured them."
            ),
            QuizQuestion(
                question = "What does the Parable of the Good Samaritan teach?",
                options = listOf("Hospitality", "Love your neighbor", "Help the poor", "All of the above"),
                correctAnswer = 3,
                explanation = "The Good Samaritan teaches us to love our neighbor by showing mercy and compassion.",
                verseReference = "Luke 10:36-37",
                verseText = "Which of these three, do you think, proved to be a neighbor to the man who fell among the robbers? He said, 'The one who showed him mercy.'"
            ),
            QuizQuestion(
                question = "What is the main point of the Parable of the Prodigal Son?",
                options = listOf("Family relationships", "God's forgiveness", "Wasting money", "Returning home"),
                correctAnswer = 1,
                explanation = "The Parable of the Prodigal Son illustrates God's unconditional love and forgiveness.",
                verseReference = "Luke 15:20",
                verseText = "And he arose and came to his father. But while he was still a long way off, his father saw him and felt compassion, and ran and embraced him and kissed him."
            ),
            QuizQuestion(
                question = "What does the Parable of the Talents teach?",
                options = listOf("Money management", "Using our gifts", "Being faithful", "All of the above"),
                correctAnswer = 3,
                explanation = "The Parable of the Talents teaches us to use our God-given gifts and abilities faithfully.",
                verseReference = "Matthew 25:21",
                verseText = "His master said to him, 'Well done, good and faithful servant. You have been faithful over a little; I will set you over much.'"
            ),
            QuizQuestion(
                question = "What is the Parable of the Lost Sheep about?",
                options = listOf("Shepherding", "God's love for sinners", "Searching", "All of the above"),
                correctAnswer = 3,
                explanation = "The Parable of the Lost Sheep illustrates God's love for sinners and His desire to save them.",
                verseReference = "Luke 15:4-6",
                verseText = "What man of you, having a hundred sheep, if he has lost one of them, does not leave the ninety-nine in the open country, and go after the one that is lost, until he finds it?"
            )
        )
    }
    
    // Placeholder functions for other topics (to be implemented)
    private fun getBiblicalHistoryQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalCharactersQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalNumbersQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalSymbolsQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalFestivalsQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalCovenantsQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalProphecyQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalTheologyQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalLanguagesQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalArchaeologyQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalInterpretationQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getAdvancedTheologyQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
    private fun getBiblicalMasteryQuestions(level: Int): List<QuizQuestion> = getEarlyChurchQuestions(level)
}
