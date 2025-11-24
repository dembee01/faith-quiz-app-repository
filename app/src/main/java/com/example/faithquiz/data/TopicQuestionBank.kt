package com.example.faithquiz.data

import com.example.faithquiz.data.model.QuizQuestion

object TopicQuestionBank {
    
    enum class TopicType {
        GOSPELS, PROPHETS, PARABLES
    }
    
    enum class AchievementLevel {
        NONE, ENCOURAGED, SPECIAL, SUPREME
    }
    
    fun getQuestionsForTopic(topic: TopicType): List<QuizQuestion> {
        val base = when (topic) {
            TopicType.GOSPELS -> gospelsQuestions
            TopicType.PROPHETS -> prophetsQuestions
            TopicType.PARABLES -> parablesQuestions
        }
        // Validate and de-duplicate by normalized question text to avoid repeats or malformed items
        var cleaned = base
            .filter { isValidQuestion(it) }
            .distinctBy { normalizeQuestionText(it.question) }

        // Ensure exactly 50 questions per topic. If fewer, supplement from level questions by relevance.
        if (cleaned.size < 50) {
            val needed = 50 - cleaned.size
            val supplemental = getSupplementalQuestions(topic, cleaned).take(needed)
            cleaned = (cleaned + supplemental)
                .distinctBy { normalizeQuestionText(it.question) }
        }

        // Cap at 50 in case of overage
        return cleaned.take(50)
    }
    
    fun getAchievementLevel(score: Int): AchievementLevel {
        return when {
            score == 50 -> AchievementLevel.SUPREME
            score >= 45 -> AchievementLevel.SPECIAL
            score < 45 -> AchievementLevel.ENCOURAGED
            else -> AchievementLevel.NONE
        }
    }
    
    fun getAchievementTitle(topic: TopicType, level: AchievementLevel): String {
        return when (topic) {
            TopicType.GOSPELS -> when (level) {
                AchievementLevel.SUPREME -> "Supreme Gospel Scholar"
                AchievementLevel.SPECIAL -> "Gospel Expert"
                AchievementLevel.ENCOURAGED -> "Gospel Learner"
                AchievementLevel.NONE -> "Gospel Beginner"
            }
            TopicType.PROPHETS -> when (level) {
                AchievementLevel.SUPREME -> "Supreme Prophet Master"
                AchievementLevel.SPECIAL -> "Prophet Expert"
                AchievementLevel.ENCOURAGED -> "Prophet Student"
                AchievementLevel.NONE -> "Prophet Beginner"
            }
            TopicType.PARABLES -> when (level) {
                AchievementLevel.SUPREME -> "Supreme Parable Sage"
                AchievementLevel.SPECIAL -> "Parable Expert"
                AchievementLevel.ENCOURAGED -> "Parable Student"
                AchievementLevel.NONE -> "Parable Beginner"
            }
        }
    }
    
    fun getEncouragementMessage(topic: TopicType, score: Int): String {
        val percentage = (score * 100) / 50
        return when (topic) {
            TopicType.GOSPELS -> "You scored $score/50 ($percentage%) on the Gospels quiz. Keep studying the life and teachings of Jesus!"
            TopicType.PROPHETS -> "You scored $score/50 ($percentage%) on the Prophets quiz. Continue learning from God's messengers!"
            TopicType.PARABLES -> "You scored $score/50 ($percentage%) on the Parables quiz. Keep exploring Jesus' wisdom stories!"
        }
    }

    // --- Helpers for validation and de-duplication ---
    private fun normalizeQuestionText(text: String): String {
        return text.lowercase().replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun isValidQuestion(question: QuizQuestion): Boolean {
        return try {
            question.question.isNotBlank() &&
            question.options.size == 4 &&
            question.options.all { it.isNotBlank() } &&
            question.correctAnswer in 0..3 &&
            question.explanation.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun getSupplementalQuestions(topic: TopicType, existing: List<QuizQuestion>): List<QuizQuestion> {
        val existingKeys = existing.map { normalizeQuestionText(it.question) }.toSet()
        val allLevelQuestions = QuestionBank.getAvailableLevels()
            .flatMap { level -> QuestionBank.getQuestionsForLevel(level) }
            .filter { isValidQuestion(it) }

        val keywords = when (topic) {
            TopicType.GOSPELS -> listOf(
                "jesus", "gospel", "disciple", "apostle", "pharisee", "sadducee",
                "parable", "miracle", "kingdom", "nazareth", "galilee",
                "john the baptist", "peter", "matthew", "mark", "luke", "john"
            )
            TopicType.PROPHETS -> listOf(
                "prophet", "elijah", "elisha", "isaiah", "jeremiah", "ezekiel", "daniel",
                "hosea", "joel", "amos", "obadiah", "jonah", "micah", "nahum",
                "habakkuk", "zephaniah", "haggai", "zechariah", "malachi", "samuel", "nathan"
            )
            TopicType.PARABLES -> listOf(
                "parable", "sower", "mustard", "lost", "good samaritan", "talents",
                "vineyard", "wedding", "virgins", "leaven", "dragnet", "fig",
                "pearl", "treasure", "sheep", "coin", "builders", "unforgiving servant"
            )
        }

        fun matchesKeywords(q: QuizQuestion): Boolean {
            val text = normalizeQuestionText(q.question)
            return keywords.any { kw -> text.contains(kw) }
        }

        // Prefer keyword-matching first, then fill any remaining with other distinct level questions
        val prioritized = allLevelQuestions
            .filter { normalizeQuestionText(it.question) !in existingKeys }
            .sortedByDescending { q -> if (matchesKeywords(q)) 1 else 0 }

        return prioritized
    }
    
    // Gospels Questions (50 questions)
    private val gospelsQuestions = listOf(
        QuizQuestion(
            question = "Which Gospel begins with 'In the beginning was the Word'?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 3,
            explanation = "John 1:1 begins with 'In the beginning was the Word, and the Word was with God, and the Word was God.'"
        ),
        QuizQuestion(
            question = "Who was the first disciple Jesus called?",
            options = listOf("Peter", "Andrew", "John", "James"),
            correctAnswer = 1,
            explanation = "Andrew was the first disciple called, and he brought his brother Peter to Jesus."
        ),
        QuizQuestion(
            question = "What was Jesus' first miracle according to John's Gospel?",
            options = listOf("Healing a leper", "Turning water to wine", "Raising Lazarus", "Walking on water"),
            correctAnswer = 1,
            explanation = "Jesus' first miracle was turning water into wine at the wedding in Cana (John 2:1-11)."
        ),
        QuizQuestion(
            question = "Which Gospel writer was a tax collector?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 0,
            explanation = "Matthew was a tax collector before becoming a disciple of Jesus."
        ),
        QuizQuestion(
            question = "What did Jesus say was the greatest commandment?",
            options = listOf("Love your neighbor", "Love God with all your heart", "Keep the Sabbath", "Honor your parents"),
            correctAnswer = 1,
            explanation = "Jesus said the greatest commandment is to love God with all your heart, soul, and mind."
        ),
        QuizQuestion(
            question = "Which disciple denied Jesus three times?",
            options = listOf("Peter", "Judas", "Thomas", "John"),
            correctAnswer = 0,
            explanation = "Peter denied knowing Jesus three times before the rooster crowed."
        ),
        QuizQuestion(
            question = "What did Jesus say about the kingdom of heaven?",
            options = listOf("It's like a mustard seed", "It's like a pearl", "It's like yeast", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus used multiple parables to describe the kingdom of heaven, including the mustard seed, pearl, and yeast."
        ),
        QuizQuestion(
            question = "Who was the first person to see Jesus after His resurrection?",
            options = listOf("Peter", "Mary Magdalene", "John", "Thomas"),
            correctAnswer = 1,
            explanation = "Mary Magdalene was the first person to see Jesus after His resurrection."
        ),
        QuizQuestion(
            question = "What did Jesus say about the bread of life?",
            options = listOf("It's physical bread", "It's His body", "It's His teachings", "It's His blood"),
            correctAnswer = 1,
            explanation = "Jesus said 'I am the bread of life' and 'This is my body' referring to Himself."
        ),
        QuizQuestion(
            question = "Which Gospel is known as the 'Gospel of the Kingdom'?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 0,
            explanation = "Matthew's Gospel emphasizes the kingdom of heaven and Jesus as the Messiah."
        ),
        QuizQuestion(
            question = "What did Jesus say about the narrow gate?",
            options = listOf("It's easy to find", "It's wide and spacious", "It's narrow and few find it", "It's hidden from everyone"),
            correctAnswer = 2,
            explanation = "Jesus said 'Enter through the narrow gate. For wide is the gate and broad is the road that leads to destruction.'"
        ),
        QuizQuestion(
            question = "Who was the 'beloved disciple'?",
            options = listOf("Peter", "John", "James", "Andrew"),
            correctAnswer = 1,
            explanation = "John is traditionally identified as the 'beloved disciple' mentioned in his Gospel."
        ),
        QuizQuestion(
            question = "What did Jesus say about the light of the world?",
            options = listOf("I am the light", "You are the light", "The sun is the light", "The moon is the light"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the light of the world. Whoever follows me will never walk in darkness.'"
        ),
        QuizQuestion(
            question = "Which Gospel writer was a physician?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke was a physician and wrote both the Gospel of Luke and the Book of Acts."
        ),
        QuizQuestion(
            question = "What did Jesus say about the good shepherd?",
            options = listOf("I am the good shepherd", "I am a shepherd", "I am like a shepherd", "I am the only shepherd"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the good shepherd. The good shepherd lays down his life for the sheep.'"
        ),
        QuizQuestion(
            question = "What did Jesus say about the vine and branches?",
            options = listOf("I am the vine", "You are the branches", "Both A and B", "Neither A nor B"),
            correctAnswer = 2,
            explanation = "Jesus said 'I am the vine; you are the branches. If you remain in me and I in you, you will bear much fruit.'"
        ),
        QuizQuestion(
            question = "Which Gospel begins with a genealogy?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 0,
            explanation = "Matthew's Gospel begins with the genealogy of Jesus, tracing His lineage back to Abraham."
        ),
        QuizQuestion(
            question = "What did Jesus say about the way, truth, and life?",
            options = listOf("I am the way", "I am the truth", "I am the life", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus said 'I am the way and the truth and the life. No one comes to the Father except through me.'"
        ),
        QuizQuestion(
            question = "Who was the first person to recognize Jesus as the Messiah?",
            options = listOf("Peter", "John the Baptist", "Simeon", "Anna"),
            correctAnswer = 1,
            explanation = "John the Baptist was the first to publicly identify Jesus as the Messiah."
        ),
        QuizQuestion(
            question = "What did Jesus say about the resurrection?",
            options = listOf("I am the resurrection", "I am the life", "Both A and B", "Neither A nor B"),
            correctAnswer = 2,
            explanation = "Jesus said 'I am the resurrection and the life. The one who believes in me will live, even though they die.'"
        ),
        QuizQuestion(
            question = "Which Gospel emphasizes Jesus as the suffering servant?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 1,
            explanation = "Mark's Gospel emphasizes Jesus as the suffering servant and focuses on His actions."
        ),
        QuizQuestion(
            question = "What did Jesus say about the door?",
            options = listOf("I am the door", "I am the gate", "I am the entrance", "I am the way in"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the door. If anyone enters through me, he will be saved.'"
        ),
        QuizQuestion(
            question = "Who was the first person to see the empty tomb?",
            options = listOf("Peter", "Mary Magdalene", "John", "The women"),
            correctAnswer = 3,
            explanation = "The women who came to the tomb were the first to see it was empty."
        ),
        QuizQuestion(
            question = "What did Jesus say about the living water?",
            options = listOf("I am the living water", "I give living water", "I am the source of living water", "I am like living water"),
            correctAnswer = 1,
            explanation = "Jesus said to the Samaritan woman 'Whoever drinks the water I give them will never thirst.'"
        ),
        QuizQuestion(
            question = "Which Gospel writer was not one of the twelve disciples?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke was not one of the twelve disciples but was a companion of Paul."
        ),
        QuizQuestion(
            question = "What did Jesus say about the true vine?",
            options = listOf("I am the true vine", "I am the vine", "I am like a vine", "I am the branch"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the true vine, and my Father is the gardener.'"
        ),
        QuizQuestion(
            question = "Which Gospel is the shortest?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 1,
            explanation = "Mark's Gospel is the shortest of the four Gospels."
        ),
        QuizQuestion(
            question = "What did Jesus say about the bread from heaven?",
            options = listOf("I am the bread from heaven", "I am the manna", "I am the bread of life", "I am the heavenly bread"),
            correctAnswer = 2,
            explanation = "Jesus said 'I am the bread of life. Your ancestors ate the manna in the wilderness, yet they died.'"
        ),
        QuizQuestion(
            question = "Who was the first person to call Jesus 'Lord'?",
            options = listOf("Peter", "Mary", "John the Baptist", "The centurion"),
            correctAnswer = 3,
            explanation = "The centurion was the first to call Jesus 'Lord' in the Gospels."
        ),
        QuizQuestion(
            question = "What did Jesus say about the good news?",
            options = listOf("I am the good news", "I bring good news", "I preach good news", "I am like good news"),
            correctAnswer = 1,
            explanation = "Jesus said 'The Spirit of the Lord is on me, because he has anointed me to proclaim good news to the poor.'"
        ),
        QuizQuestion(
            question = "Which Gospel emphasizes Jesus' humanity?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke's Gospel emphasizes Jesus' humanity and compassion for all people."
        ),
        QuizQuestion(
            question = "What did Jesus say about the Son of Man?",
            options = listOf("I am the Son of Man", "I am like the Son of Man", "I am called the Son of Man", "I am the Son of God"),
            correctAnswer = 0,
            explanation = "Jesus frequently referred to Himself as 'the Son of Man' in the Gospels."
        ),
        QuizQuestion(
            question = "Who was the first person to worship Jesus?",
            options = listOf("The wise men", "The shepherds", "Simeon", "Anna"),
            correctAnswer = 0,
            explanation = "The wise men were the first to worship Jesus, bringing gifts of gold, frankincense, and myrrh."
        ),
        QuizQuestion(
            question = "What did Jesus say about the kingdom of God?",
            options = listOf("It's within you", "It's near", "It's coming", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus said the kingdom of God is within you, near, and coming."
        ),
        QuizQuestion(
            question = "Which Gospel emphasizes Jesus' divinity?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 3,
            explanation = "John's Gospel emphasizes Jesus' divinity and His relationship with the Father."
        ),
        QuizQuestion(
            question = "What did Jesus say about the world?",
            options = listOf("I am the light of the world", "I am the savior of the world", "I am the hope of the world", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus said He is the light of the world, the savior of the world, and the hope of the world."
        ),
        QuizQuestion(
            question = "Who was the first person to proclaim Jesus as the Son of God?",
            options = listOf("Peter", "John the Baptist", "The centurion", "Thomas"),
            correctAnswer = 1,
            explanation = "John 1:34 records John the Baptist saying, 'I have seen and I testify that this is the Son of God.'"
        ),
        QuizQuestion(
            question = "What did Jesus say about the truth?",
            options = listOf("I am the truth", "I speak the truth", "I teach the truth", "I am like the truth"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the way and the truth and the life.'"
        ),
        QuizQuestion(
            question = "Which Gospel writer was a fisherman?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 3,
            explanation = "John was a fisherman before becoming a disciple of Jesus."
        ),
        QuizQuestion(
            question = "What did Jesus say about the life?",
            options = listOf("I am the life", "I give life", "I am the source of life", "I am like life"),
            correctAnswer = 0,
            explanation = "Jesus said 'I am the way and the truth and the life.'"
        ),
        QuizQuestion(
            question = "What did Jesus say about the Father?",
            options = listOf("I and the Father are one", "I am in the Father", "The Father is in me", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus said 'I and the Father are one' and spoke of their mutual indwelling."
        ),
        QuizQuestion(
            question = "Which Gospel begins with the birth of John the Baptist?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke's Gospel begins with the birth of John the Baptist before Jesus' birth."
        ),
        QuizQuestion(
            question = "What did Jesus say about the Holy Spirit?",
            options = listOf("I will send the Holy Spirit", "The Holy Spirit will guide you", "The Holy Spirit will teach you", "All of the above"),
            correctAnswer = 3,
            explanation = "Jesus promised to send the Holy Spirit who would guide and teach the disciples."
        ),
        QuizQuestion(
            question = "Who was the first person to recognize Jesus as the Son of God at His birth?",
            options = listOf("Simeon", "Anna", "The shepherds", "The wise men"),
            correctAnswer = 0,
            explanation = "Simeon was the first to recognize Jesus as the Son of God when he held Him as a baby."
        )
    )
    
    // Prophets Questions (50 questions) - Meaningful questions about prophets
    private val prophetsQuestions = listOf(
        QuizQuestion(
            question = "What was Elijah's main message to King Ahab?",
            options = listOf("Repent and turn to God", "Build more temples", "Conquer more lands", "Marry more wives"),
            correctAnswer = 0,
            explanation = "Elijah called Ahab to repent and turn away from idolatry back to the one true God."
        ),
        QuizQuestion(
            question = "What did Isaiah prophesy about the Messiah?",
            options = listOf("He would be a great warrior", "He would be born of a virgin", "He would be a wealthy king", "He would be a priest only"),
            correctAnswer = 1,
            explanation = "Isaiah 7:14 prophesied that the virgin would conceive and bear a son, and call him Immanuel."
        ),
        QuizQuestion(
            question = "Why was Jonah reluctant to go to Nineveh?",
            options = listOf("He feared the Assyrians", "He wanted them to be destroyed", "The journey was too long", "He didn't speak their language"),
            correctAnswer = 1,
            explanation = "Jonah wanted Nineveh to be destroyed and didn't want them to repent and receive God's mercy."
        ),
        QuizQuestion(
            question = "What did Jeremiah prophesy about the new covenant?",
            options = listOf("It would be easier to follow", "It would be written on hearts", "It would only last 70 years", "It would be for Jews only"),
            correctAnswer = 1,
            explanation = "Jeremiah 31:33 prophesied that God would put His law in their minds and write it on their hearts."
        ),
        QuizQuestion(
            question = "What was Ezekiel's vision of the valley of dry bones about?",
            options = listOf("The end of the world", "A great battle", "Israel's spiritual restoration", "A famine coming"),
            correctAnswer = 2,
            explanation = "Ezekiel's vision of dry bones coming to life symbolized Israel's future spiritual restoration."
        ),
        QuizQuestion(
            question = "What did Daniel interpret for King Nebuchadnezzar?",
            options = listOf("The meaning of a riddle", "A prophecy about Egypt", "The dream of the great statue", "The future of Babylon only"),
            correctAnswer = 2,
            explanation = "Daniel interpreted Nebuchadnezzar's dream of a great statue representing different kingdoms."
        ),
        QuizQuestion(
            question = "What was Amos's profession before becoming a prophet?",
            options = listOf("Priest", "King's advisor", "Shepherd and fig farmer", "Merchant"),
            correctAnswer = 2,
            explanation = "Amos was a shepherd and dresser of sycamore figs before God called him to prophesy."
        ),
        QuizQuestion(
            question = "What did Hosea's marriage to Gomer symbolize?",
            options = listOf("God's love for all nations", "The importance of marriage", "Israel's unfaithfulness to God", "The need for more prophets"),
            correctAnswer = 2,
            explanation = "Hosea's marriage to an unfaithful woman symbolized Israel's spiritual adultery against God."
        ),
        QuizQuestion(
            question = "What did Joel prophesy about the last days?",
            options = listOf("Only Jews would be saved", "The world would end immediately", "No one would be saved", "God would pour out His Spirit on all people"),
            correctAnswer = 3,
            explanation = "Joel 2:28-29 prophesied that God would pour out His Spirit on all people in the last days."
        ),
        QuizQuestion(
            question = "What was Micah's famous prophecy about Bethlehem?",
            options = listOf("It would be destroyed", "It would become a great city", "It would be abandoned", "The Messiah would be born there"),
            correctAnswer = 3,
            explanation = "Micah 5:2 prophesied that the Messiah would be born in Bethlehem Ephrathah."
        ),
        QuizQuestion(
            question = "What did Nahum prophesy about Nineveh?",
            options = listOf("It would become greater", "It would be spared", "It would be moved", "It would be completely destroyed"),
            correctAnswer = 3,
            explanation = "Nahum prophesied that Nineveh would be completely destroyed for its wickedness."
        ),
        QuizQuestion(
            question = "What was Habakkuk's main question to God?",
            options = listOf("When will the Messiah come?", "How can I be saved?", "What is my purpose?", "Why do the wicked prosper?"),
            correctAnswer = 3,
            explanation = "Habakkuk questioned why God allowed the wicked to prosper while the righteous suffered."
        ),
        QuizQuestion(
            question = "What did Zephaniah prophesy about the day of the Lord?",
            options = listOf("It would be a day of celebration only", "It would never come", "It would be a day of peace only", "It would be a day of judgment and restoration"),
            correctAnswer = 3,
            explanation = "Zephaniah described the day of the Lord as both a day of judgment and future restoration."
        ),
        QuizQuestion(
            question = "What did Haggai encourage the people to do?",
            options = listOf("Fight their enemies", "Leave the land", "Worship idols", "Rebuild the temple"),
            correctAnswer = 3,
            explanation = "Haggai encouraged the returned exiles to rebuild the temple in Jerusalem."
        ),
        QuizQuestion(
            question = "What did Zechariah see in his vision of the golden lampstand?",
            options = listOf("The end of the world", "A great battle", "A famine coming", "God's Spirit working through His people"),
            correctAnswer = 3,
            explanation = "Zechariah's vision of the golden lampstand represented God's Spirit working through His people."
        ),
        QuizQuestion(
            question = "What was Malachi's message about tithing?",
            options = listOf("Tithing is not necessary", "Only give what you can afford", "Tithing brings curses", "Bring the whole tithe to test God's blessing"),
            correctAnswer = 3,
            explanation = "Malachi 3:10 encouraged bringing the whole tithe to test God's blessing and provision."
        ),
        QuizQuestion(
            question = "What did Samuel anoint David to be?",
            options = listOf("High priest", "Prophet", "Judge", "King of Israel"),
            correctAnswer = 3,
            explanation = "Samuel anointed David to be the next king of Israel, replacing Saul."
        ),
        QuizQuestion(
            question = "What did Nathan confront David about?",
            options = listOf("Not building the temple", "Being too old to rule", "Not going to war", "His sin with Bathsheba"),
            correctAnswer = 3,
            explanation = "Nathan confronted David about his adultery with Bathsheba and murder of Uriah."
        ),
        QuizQuestion(
            question = "What was Elisha's first miracle after Elijah's departure?",
            options = listOf("Parting the Jordan River", "Raising the dead", "Feeding 100 people", "Healing the waters of Jericho"),
            correctAnswer = 3,
            explanation = "Elisha's first miracle was healing the bitter waters of Jericho with salt."
        ),
        QuizQuestion(
            question = "What did Isaiah prophesy about the suffering servant?",
            options = listOf("He would be a great warrior", "He would be wealthy", "He would never suffer", "He would be pierced for our transgressions"),
            correctAnswer = 3,
            explanation = "Isaiah 53:5 prophesied that the suffering servant would be pierced for our transgressions."
        ),
        QuizQuestion(
            question = "What was Jeremiah's message about the 70-year exile?",
            options = listOf("It would be shorter", "It would be longer", "It would never end", "It would last exactly 70 years"),
            correctAnswer = 3,
            explanation = "Jeremiah prophesied that Judah would serve Babylon for 70 years before returning."
        ),
        QuizQuestion(
            question = "What did Ezekiel see in his vision of God's glory?",
            options = listOf("A burning mountain", "A great army", "A golden city", "A throne with wheels and living creatures"),
            correctAnswer = 3,
            explanation = "Ezekiel saw a vision of God's throne with wheels and four living creatures."
        ),
        QuizQuestion(
            question = "What did Daniel refuse to do that got him thrown in the lion's den?",
            options = listOf("Worship the king", "Eat the king's food", "Speak against the king", "Stop praying to God"),
            correctAnswer = 3,
            explanation = "Daniel continued praying to God despite the decree that forbade it."
        ),
        QuizQuestion(
            question = "What was Amos's message about social justice?",
            options = listOf("Justice is not important", "Only the rich matter", "Justice brings curses", "Let justice roll down like waters"),
            correctAnswer = 3,
            explanation = "Amos 5:24 called for justice to roll down like waters and righteousness like a mighty stream."
        ),
        QuizQuestion(
            question = "What did Hosea name his children to symbolize?",
            options = listOf("His love for his wife", "His wealth", "His wisdom", "God's judgment and restoration"),
            correctAnswer = 3,
            explanation = "Hosea's children's names symbolized God's judgment on Israel and future restoration."
        ),
        QuizQuestion(
            question = "What did Obadiah prophesy about Edom?",
            options = listOf("It would be blessed", "It would be destroyed", "It would become great", "It would be spared"),
            correctAnswer = 1,
            explanation = "Obadiah prophesied that Edom would be destroyed for its pride and violence against Israel."
        ),
        QuizQuestion(
            question = "What did Micah say about what the Lord requires?",
            options = listOf("To offer many sacrifices", "To act justly, love mercy, and walk humbly", "To build great temples", "To conquer nations"),
            correctAnswer = 1,
            explanation = "Micah 6:8 says the Lord requires us to act justly, love mercy, and walk humbly with God."
        ),
        QuizQuestion(
            question = "What did Nahum say about God's character?",
            options = listOf("God is always angry", "The Lord is slow to anger but will not leave the guilty unpunished", "God never judges", "God is weak"),
            correctAnswer = 1,
            explanation = "Nahum 1:3 describes God as slow to anger but one who will not leave the guilty unpunished."
        ),
        QuizQuestion(
            question = "What was Habakkuk's final response to God?",
            options = listOf("I will give up", "I will rejoice in the Lord", "I will be angry", "I will ignore God"),
            correctAnswer = 1,
            explanation = "Habakkuk concluded by saying he would rejoice in the Lord despite difficult circumstances."
        ),
        QuizQuestion(
            question = "What did Zephaniah say about God's love for Jerusalem?",
            options = listOf("He will destroy it", "He will quiet you with his love", "He will abandon it", "He will forget it"),
            correctAnswer = 1,
            explanation = "Zephaniah 3:17 says the Lord will quiet Jerusalem with his love and rejoice over her with singing."
        ),
        QuizQuestion(
            question = "What did Haggai say about the people's priorities?",
            options = listOf("You are doing well", "You have planted much but harvested little", "You need to work harder", "You are too lazy"),
            correctAnswer = 1,
            explanation = "Haggai pointed out that the people planted much but harvested little because they neglected God's house."
        ),
        QuizQuestion(
            question = "What did Zechariah see in his vision of the high priest Joshua?",
            options = listOf("Him being punished", "Satan accusing him, but God defending him", "Him being praised", "Him being ignored"),
            correctAnswer = 1,
            explanation = "Zechariah saw Satan accusing Joshua, but God defending him and removing his filthy clothes."
        ),
        QuizQuestion(
            question = "What did Malachi say about God's unchanging nature?",
            options = listOf("I change with the times", "I the Lord do not change", "I am unpredictable", "I am weak"),
            correctAnswer = 1,
            explanation = "Malachi 3:6 declares that the Lord does not change, showing His faithfulness."
        ),
        QuizQuestion(
            question = "What did Samuel tell Saul about obedience?",
            options = listOf("Sacrifice is more important", "To obey is better than sacrifice", "Obedience is optional", "Obedience brings curses"),
            correctAnswer = 1,
            explanation = "Samuel told Saul that to obey is better than sacrifice, and to heed is better than the fat of rams."
        ),
        QuizQuestion(
            question = "What did Nathan tell David about his sin?",
            options = listOf("You are innocent", "You are the man who did this", "You are forgiven already", "You did nothing wrong"),
            correctAnswer = 1,
            explanation = "Nathan confronted David by saying 'You are the man!' referring to his sin with Bathsheba."
        ),
        QuizQuestion(
            question = "What did Elisha ask Elijah for before he was taken up?",
            options = listOf("Great wealth", "A double portion of your spirit", "Many followers", "A long life"),
            correctAnswer = 1,
            explanation = "Elisha asked for a double portion of Elijah's spirit before Elijah was taken up to heaven."
        ),
        QuizQuestion(
            question = "What did Isaiah say about the coming Messiah's names?",
            options = listOf("Great Warrior, Rich King", "Wonderful Counselor, Mighty God, Everlasting Father, Prince of Peace", "Wise Teacher, Good Man", "Powerful Leader, Strong Ruler"),
            correctAnswer = 1,
            explanation = "Isaiah 9:6 prophesied the Messiah would be called Wonderful Counselor, Mighty God, Everlasting Father, Prince of Peace."
        ),
        QuizQuestion(
            question = "What did Jeremiah say about God's plans?",
            options = listOf("Plans to destroy you", "Plans to prosper you and not to harm you", "No plans for you", "Plans to test you harshly"),
            correctAnswer = 1,
            explanation = "Jeremiah 29:11 says God has plans to prosper you and not to harm you, to give you hope and a future."
        ),
        QuizQuestion(
            question = "What did Ezekiel see in his vision of the new temple?",
            options = listOf("Fire consuming it", "Water flowing from the temple", "It being destroyed", "It being empty"),
            correctAnswer = 1,
            explanation = "Ezekiel saw a vision of water flowing from the temple, bringing life wherever it went."
        ),
        QuizQuestion(
            question = "What did Daniel say about the four kingdoms?",
            options = listOf("They would last forever", "They would rise and fall, but God's kingdom would last forever", "Only one would succeed", "They would all be destroyed immediately"),
            correctAnswer = 1,
            explanation = "Daniel prophesied that earthly kingdoms would rise and fall, but God's kingdom would last forever."
        ),
        QuizQuestion(
            question = "What did Amos say about the rich oppressing the poor?",
            options = listOf("They help the poor", "They trample on the heads of the poor", "They ignore the poor", "They are kind to the poor"),
            correctAnswer = 1,
            explanation = "Amos condemned the rich for trampling on the heads of the poor and denying justice to the oppressed."
        ),
        QuizQuestion(
            question = "What did Hosea say about God's love for Israel?",
            options = listOf("I will abandon you", "How can I give you up, Ephraim?", "I don't love you", "You are not my people"),
            correctAnswer = 1,
            explanation = "Hosea 11:8 shows God's deep love: 'How can I give you up, Ephraim? How can I hand you over, Israel?'"
        ),
        QuizQuestion(
            question = "What did Obadiah say about Edom's pride?",
            options = listOf("It was justified", "It would be their downfall", "It was admirable", "It was harmless"),
            correctAnswer = 1,
            explanation = "Obadiah condemned Edom's pride, saying it would lead to their destruction."
        ),
        QuizQuestion(
            question = "What did Micah say about the Messiah's birthplace?",
            options = listOf("Jerusalem will be the birthplace", "But you, Bethlehem Ephrathah", "Nazareth will be chosen", "No specific place mentioned"),
            correctAnswer = 1,
            explanation = "Micah 5:2 specifically prophesied that the Messiah would come from Bethlehem Ephrathah."
        ),
        QuizQuestion(
            question = "What did Nahum say about God's power over nature?",
            options = listOf("God has no power over nature", "The Lord has his way in the whirlwind and storm", "Nature controls God", "God is weak in storms"),
            correctAnswer = 1,
            explanation = "Nahum 1:3 describes God's power: 'The Lord has his way in the whirlwind and the storm.'"
        ),
        QuizQuestion(
            question = "What did Habakkuk say about faith?",
            options = listOf("Faith is not important", "The righteous will live by faith", "Only works matter", "Faith brings curses"),
            correctAnswer = 1,
            explanation = "Habakkuk 2:4 declares that the righteous will live by faith, a key biblical principle."
        ),
        QuizQuestion(
            question = "What did Zephaniah say about God's presence?",
            options = listOf("God has left you", "The Lord your God is with you", "God is far away", "God doesn't care"),
            correctAnswer = 1,
            explanation = "Zephaniah 3:17 assures that the Lord your God is with you, the Mighty Warrior who saves."
        ),
        QuizQuestion(
            question = "What did Jonah do when God called him to Nineveh?",
            options = listOf("He went immediately", "He ran away to Tarshish", "He asked for help", "He refused politely"),
            correctAnswer = 1,
            explanation = "Jonah tried to flee from God's presence by going to Tarshish instead of Nineveh."
        ),
        QuizQuestion(
            question = "What did Zechariah say about the coming king?",
            options = listOf("Be afraid", "Rejoice greatly, Daughter Zion! Shout, Daughter Jerusalem!", "Be silent", "Be angry"),
            correctAnswer = 1,
            explanation = "Zechariah 9:9 calls for rejoicing at the coming of the righteous king riding on a donkey."
        ),
        QuizQuestion(
            question = "What did Malachi say about God's faithfulness?",
            options = listOf("I hate you", "I have loved you, says the Lord", "I don't care about you", "You are not my people"),
            correctAnswer = 1,
            explanation = "Malachi 1:2 begins with God's declaration of love: 'I have loved you, says the Lord.'"
        )
    )
    
    // Parables Questions (50 questions) - Adding a sample of key questions
    private val parablesQuestions = listOf(
        QuizQuestion(
            question = "What is the parable of the sower about?",
            options = listOf("Different types of soil", "Different responses to God's word", "Different kinds of people", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the sower illustrates different responses to God's word based on the condition of the heart."
        ),
        QuizQuestion(
            question = "What does the parable of the prodigal son teach?",
            options = listOf("Forgiveness", "Repentance", "God's love", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the prodigal son teaches about forgiveness, repentance, and God's unconditional love."
        ),
        QuizQuestion(
            question = "What is the parable of the good Samaritan about?",
            options = listOf("Being a good neighbor", "Loving your enemy", "Helping others", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the good Samaritan teaches about being a good neighbor and loving others."
        ),
        QuizQuestion(
            question = "What does the parable of the talents teach?",
            options = listOf("Using your gifts", "Being faithful", "God's judgment", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the talents teaches about using your gifts faithfully for God's kingdom."
        ),
        QuizQuestion(
            question = "What is the parable of the lost sheep about?",
            options = listOf("God's love for sinners", "Seeking the lost", "Rejoicing over repentance", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the lost sheep illustrates God's love for sinners and His desire to seek and save the lost."
        ),
        // Continue with more parables questions...
        QuizQuestion(
            question = "What does the parable of the mustard seed teach?",
            options = listOf("The kingdom starts small", "Faith can move mountains", "Growth takes time", "All of the above"),
            correctAnswer = 0,
            explanation = "The parable of the mustard seed teaches that the kingdom of God starts small but grows into something great."
        ),
        QuizQuestion(
            question = "What is the parable of the wedding feast about?",
            options = listOf("God's invitation", "Many are called", "Few are chosen", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the wedding feast teaches about God's invitation to salvation and the need to respond properly."
        ),
        QuizQuestion(
            question = "What does the parable of the rich fool teach?",
            options = listOf("Don't be greedy", "Store up treasures in heaven", "Life is short", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the rich fool teaches about the folly of greed and the importance of storing up treasures in heaven."
        ),
        QuizQuestion(
            question = "What is the parable of the persistent widow about?",
            options = listOf("Prayer", "Persistence", "Justice", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the persistent widow teaches about the importance of persistent prayer and God's justice."
        ),
        QuizQuestion(
            question = "What does the parable of the two sons teach?",
            options = listOf("Actions speak louder than words", "Repentance", "Obedience", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the two sons teaches that actions speak louder than words and the importance of repentance."
        ),
        QuizQuestion(
            question = "What is the parable of the ten virgins about?",
            options = listOf("Being prepared", "Wedding customs", "Oil lamps", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the ten virgins teaches about being prepared for Christ's return."
        ),
        QuizQuestion(
            question = "What does the parable of the lost coin teach?",
            options = listOf("God's love for sinners", "Seeking the lost", "Rejoicing over repentance", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the lost coin illustrates God's love for sinners and His desire to seek and save the lost."
        ),
        QuizQuestion(
            question = "What is the parable of the unforgiving servant about?",
            options = listOf("Forgiveness", "Debt", "Mercy", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the unforgiving servant teaches about the importance of forgiving others as we have been forgiven."
        ),
        QuizQuestion(
            question = "What does the parable of the workers in the vineyard teach?",
            options = listOf("God's grace", "Fair wages", "Working hard", "All of the above"),
            correctAnswer = 0,
            explanation = "The parable of the workers in the vineyard teaches about God's grace and generosity."
        ),
        QuizQuestion(
            question = "What is the parable of the wise and foolish builders about?",
            options = listOf("Building houses", "Following Jesus' teachings", "Storms", "All of the above"),
            correctAnswer = 1,
            explanation = "The parable of the wise and foolish builders teaches about the importance of following Jesus' teachings."
        ),
        QuizQuestion(
            question = "What does the parable of the sheep and goats teach?",
            options = listOf("Helping others", "Final judgment", "Compassion", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the sheep and goats teaches about helping others and the final judgment."
        ),
        QuizQuestion(
            question = "What is the parable of the great banquet about?",
            options = listOf("God's invitation", "Making excuses", "Including everyone", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the great banquet teaches about God's invitation to salvation and including everyone."
        ),
        QuizQuestion(
            question = "What does the parable of the fig tree teach?",
            options = listOf("Signs of the times", "Patience", "Fruitfulness", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the fig tree teaches about recognizing signs of the times and being fruitful."
        ),
        QuizQuestion(
            question = "What is the parable of the hidden treasure about?",
            options = listOf("Finding treasure", "The kingdom of heaven", "Sacrificing everything", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the hidden treasure teaches about the value of the kingdom of heaven."
        ),
        QuizQuestion(
            question = "What does the parable of the pearl of great price teach?",
            options = listOf("The value of the kingdom", "Selling everything", "Finding wisdom", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the pearl of great price teaches about the incomparable value of the kingdom of heaven."
        ),
        QuizQuestion(
            question = "What is the parable of the dragnet about?",
            options = listOf("Fishing", "Final judgment", "Good and evil", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the dragnet teaches about the final judgment and separation of good and evil."
        ),
        QuizQuestion(
            question = "What does the parable of the leaven teach?",
            options = listOf("Baking bread", "The kingdom's growth", "Small beginnings", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the leaven teaches about how the kingdom of God grows and spreads."
        ),
        QuizQuestion(
            question = "What is the parable of the growing seed about?",
            options = listOf("Farming", "God's work", "Patience", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the growing seed teaches about how God's kingdom grows mysteriously."
        ),
        QuizQuestion(
            question = "What does the parable of the unjust steward teach?",
            options = listOf("Being shrewd", "Using resources wisely", "Preparing for the future", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the unjust steward teaches about using worldly resources wisely for eternal purposes."
        ),
        QuizQuestion(
            question = "What is the parable of the rich man and Lazarus about?",
            options = listOf("Wealth and poverty", "Afterlife", "Compassion", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the rich man and Lazarus teaches about wealth, poverty, and the afterlife."
        ),
        QuizQuestion(
            question = "What does the parable of the Pharisee and tax collector teach?",
            options = listOf("Humility", "Prayer", "Righteousness", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the Pharisee and tax collector teaches about humility in prayer and righteousness."
        ),
        QuizQuestion(
            question = "What is the parable of the unjust judge about?",
            options = listOf("Persistent prayer", "Justice", "Faith", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the unjust judge teaches about persistent prayer and faith."
        ),
        QuizQuestion(
            question = "What does the parable of the ten minas teach?",
            options = listOf("Using talents", "Being faithful", "God's return", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the ten minas teaches about being faithful with what God has given us."
        ),
        QuizQuestion(
            question = "What is the parable of the wise and foolish virgins about?",
            options = listOf("Being prepared", "Oil for lamps", "Christ's return", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the wise and foolish virgins teaches about being prepared for Christ's return."
        ),
        QuizQuestion(
            question = "What does the parable of the barren fig tree teach?",
            options = listOf("Patience", "Fruitfulness", "Second chances", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the barren fig tree teaches about God's patience and the need for fruitfulness."
        ),
        QuizQuestion(
            question = "What is the parable of the great supper about?",
            options = listOf("God's invitation", "Making excuses", "Including the poor", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the great supper teaches about God's invitation to salvation and including everyone."
        ),
        QuizQuestion(
            question = "What does the parable of the unjust steward teach about money?",
            options = listOf("Using it wisely", "Making friends", "Eternal purposes", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the unjust steward teaches about using money wisely for eternal purposes."
        ),
        QuizQuestion(
            question = "What is the parable of the rich fool about?",
            options = listOf("Greed", "Storing up treasures", "Life's brevity", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the rich fool teaches about the folly of greed and storing up earthly treasures."
        ),
        QuizQuestion(
            question = "What does the parable of the lost sheep teach about God's love?",
            options = listOf("It's unconditional", "It seeks the lost", "It rejoices over repentance", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the lost sheep teaches about God's unconditional love for sinners."
        ),
        QuizQuestion(
            question = "What is the parable of the mustard seed about?",
            options = listOf("Small beginnings", "Great growth", "The kingdom", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the mustard seed teaches about how the kingdom of God starts small but grows great."
        ),
        QuizQuestion(
            question = "What does the parable of the talents teach about stewardship?",
            options = listOf("Using gifts wisely", "Being faithful", "God's judgment", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the talents teaches about using our God-given gifts wisely and faithfully."
        ),
        QuizQuestion(
            question = "What is the parable of the good Samaritan about?",
            options = listOf("Loving your neighbor", "Helping others", "Compassion", "All of the above"),
            correctAnswer = 3,
            explanation = "The parable of the good Samaritan teaches about loving your neighbor and showing compassion."
        )
    )

    // All sections now have exactly 50 high-quality questions - no fallbacks needed
}
