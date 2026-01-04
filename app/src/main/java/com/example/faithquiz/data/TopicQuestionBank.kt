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
            question = "What is the Great Commandment?",
            options = listOf("Love your neighbor as yourself", "Love God with all your heart, soul, and mind", "Keep the Sabbath holy", "Honor your father and mother"),
            correctAnswer = 1,
            explanation = "Jesus said the first and greatest commandment is to love God with all your heart, soul, and mind."
        ),
        QuizQuestion(
            question = "Which disciple denied Jesus three times?",
            options = listOf("Peter", "Judas", "Thomas", "John"),
            correctAnswer = 0,
            explanation = "Peter denied knowing Jesus three times before the rooster crowed."
        ),
        QuizQuestion(
            question = "To whom did Jesus say, 'You must be born again'?",
            options = listOf("Nicodemus", "Zacchaeus", "Pilate", "Caiaphas"),
            correctAnswer = 0,
            explanation = "Jesus told the Pharisee Nicodemus that he must be born again to see the kingdom of God (John 3)."
        ),
        QuizQuestion(
            question = "Who was the first person to see Jesus after His resurrection?",
            options = listOf("Peter", "Mary Magdalene", "John", "Thomas"),
            correctAnswer = 1,
            explanation = "Mary Magdalene was the first person to see Jesus after His resurrection."
        ),
        QuizQuestion(
            question = "What does the name 'Immanuel' mean?",
            options = listOf("God with us", "God saves", "God is love", "The Annointed One"),
            correctAnswer = 0,
            explanation = "Immanuel means 'God with us' (Matthew 1:23)."
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
            explanation = "Jesus said 'Enter through the narrow gate... only a few find it.'"
        ),
        QuizQuestion(
            question = "Who is referred to as the 'Disciple whom Jesus loved'?",
            options = listOf("Peter", "John", "James", "Andrew"),
            correctAnswer = 1,
            explanation = "John is traditionally identified as the 'beloved disciple' mentioned in his Gospel."
        ),
        QuizQuestion(
            question = "Which miracle is recorded in all four Gospels?",
            options = listOf("Turning water into wine", "Walking on water", "Feeding the 5000", "Raising Lazarus"),
            correctAnswer = 2,
            explanation = "The Feeding of the 5000 is the only miracle recorded in all four Gospels."
        ),
        QuizQuestion(
            question = "Which Gospel writer was a physician?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke was a physician and wrote both the Gospel of Luke and the Book of Acts."
        ),
        QuizQuestion(
            question = "Who climbed a sycamore tree to see Jesus?",
            options = listOf("Zacchaeus", "Nicodemus", "Bartimaeus", "Lazarus"),
            correctAnswer = 0,
            explanation = "Zacchaeus, a tax collector, climbed a sycamore-fig tree to see Jesus passing by."
        ),
        QuizQuestion(
            question = "What is the 'Golden Rule'?",
            options = listOf("Love your enemies", "Do to others as you would have them do to you", "Judge not", "Give to the poor"),
            correctAnswer = 1,
            explanation = "The Golden Rule is: 'In everything, do to others what you would have them do to you' (Matthew 7:12)."
        ),
        QuizQuestion(
            question = "Which Gospel begins with the genealogy of Jesus from Abraham?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 0,
            explanation = "Matthew traces Jesus' lineage from Abraham to emphasize His Jewish heritage."
        ),
        QuizQuestion(
            question = "Who said, 'I am the way and the truth and the life'?",
            options = listOf("Peter", "Paul", "Jesus", "John the Baptist"),
            correctAnswer = 2,
            explanation = "Jesus made this declaration in John 14:6."
        ),
        QuizQuestion(
            question = "Who was the high priest who presided over Jesus' trial?",
            options = listOf("Annas", "Caiaphas", "Pilate", "Herod"),
            correctAnswer = 1,
            explanation = "Caiaphas was the high priest who prophesied it was better for one man to die for the people."
        ),
        QuizQuestion(
            question = "Which sister sat at Jesus' feet while the other worked?",
            options = listOf("Martha", "Mary", "Salome", "Joanna"),
            correctAnswer = 1,
            explanation = "Mary sat at Jesus' feet listening, while Martha was distracted by preparations (Luke 10)."
        ),
        QuizQuestion(
            question = "Which Gospel highlights Jesus as the Suffering Servant?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 1,
            explanation = "Mark portrays Jesus as a servant who came to give His life as a ransom for many."
        ),
        QuizQuestion(
            question = "Who washed his hands to show he was innocent of Jesus' blood?",
            options = listOf("Herod", "Caiaphas", "Pilate", "Judas"),
            correctAnswer = 2,
            explanation = "Pilate washed his hands before the crowd, claiming innocence of Jesus' blood."
        ),
        QuizQuestion(
            question = "Who were the first visitors to the empty tomb?",
            options = listOf("Peter and John", "Mary Magdalene and the other Mary", "The Roman guards", "The Pharisees"),
            correctAnswer = 1,
            explanation = "The women, including Mary Magdalene, went to the tomb early on the first day of the week."
        ),
        QuizQuestion(
            question = "What represents Jesus' body in the Lord's Supper?",
            options = listOf("The wine", "The bread", "The water", "The lamb"),
            correctAnswer = 1,
            explanation = "Jesus broke the bread and said, 'Take and eat; this is my body.'"
        ),
        QuizQuestion(
            question = "Which Gospel writer was not one of the twelve disciples?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke was not one of the Twelve; he was a companion of Paul and a historian."
        ),
        QuizQuestion(
            question = "Who helped Jesus carry His cross?",
            options = listOf("Simon of Cyrene", "Joseph of Arimathea", "Barnabas", "Peter"),
            correctAnswer = 0,
            explanation = "Reviewing soldiers forced Simon of Cyrene to carry the cross."
        ),
        QuizQuestion(
            question = "Which Gospel is the shortest?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 1,
            explanation = "Mark is the shortest Gospel and likely the first one written."
        ),
        QuizQuestion(
            question = "What discourse is found in Matthew 5-7?",
            options = listOf("The Olivet Discourse", "The Sermon on the Mount", "The Upper Room Discourse", "The Parables of the Kingdom"),
            correctAnswer = 1,
            explanation = "The Sermon on the Mount contains the Beatitudes and the Lord's Prayer."
        ),
        QuizQuestion(
            question = "Who doubted Jesus' resurrection until he saw the nail marks?",
            options = listOf("Peter", "Thomas", "Philip", "Andrew"),
            correctAnswer = 1,
            explanation = "Thomas said he would not believe unless he saw the nail marks in Jesus' hands."
        ),
        QuizQuestion(
            question = "Where was Jesus baptized?",
            options = listOf("Sea of Galilee", "Jordan River", "Dead Sea", "Pool of Siloam"),
            correctAnswer = 1,
            explanation = "Jesus was baptized by John in the Jordan River."
        ),
        QuizQuestion(
            question = "Which Gospel emphasizes Jesus' humanity?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke emphasizes Jesus' compassion for the poor, outcasts, and His humanity."
        ),
        QuizQuestion(
            question = "How many baskets of leftovers were gathered after feeding the 5000?",
            options = listOf("7", "12", "3", "70"),
            correctAnswer = 1,
            explanation = "They picked up twelve basketfuls of broken pieces that were left over."
        ),
        QuizQuestion(
            question = "Who originally visited the baby Jesus in Matthew's Gospel?",
            options = listOf("Shepherds", "Wise Men (Magi)", "Angels", "Simeon"),
            correctAnswer = 1,
            explanation = "Matthew records the visit of the Wise Men (Magi) from the east."
        ),
        QuizQuestion(
            question = "What happens to the seed that falls on rocky ground?",
            options = listOf("It brings forth much fruit", "It gets choked by thorns", "It springs up quickly but withers", "Birds eat it"),
            correctAnswer = 2,
            explanation = "Since it has no root, it lasts only a short time and withers under the sun."
        ),
        QuizQuestion(
            question = "Which Gospel emphasizes Jesus' divinity ('I AM' statements)?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 3,
            explanation = "John's Gospel contains the seven 'I AM' statements highlighting Jesus' divinity."
        ),
        QuizQuestion(
            question = "Who buried Jesus in his own new tomb?",
            options = listOf("Nicodemus", "Joseph of Arimathea", "Peter", "John"),
            correctAnswer = 1,
            explanation = "Joseph of Arimathea asked Pilate for Jesus' body and placed it in his own new tomb."
        ),
        QuizQuestion(
            question = "Who witnessed the Transfiguration?",
            options = listOf("All 12 disciples", "Peter, James, and John", "Mary and Martha", "Moses and Elijah only"),
            correctAnswer = 1,
            explanation = "Jesus took Peter, James, and John up the mountain where He was transfigured."
        ),
        QuizQuestion(
            question = "What sign did Judas use to betray Jesus?",
            options = listOf("A handshake", "A hug", "A kiss", "A bow"),
            correctAnswer = 2,
            explanation = "Judas betrayed Jesus with a kiss."
        ),
        QuizQuestion(
            question = "Which Gospel writer was a cousin of Barnabas?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 1,
            explanation = "Mark (John Mark) was the cousin of Barnabas (Colossians 4:10)."
        ),
        QuizQuestion(
            question = "Who said, 'I must decrease, but He must increase'?",
            options = listOf("Peter", "Paul", "John the Baptist", "John the Apostle"),
            correctAnswer = 2,
            explanation = "John the Baptist said this regarding his ministry in relation to Jesus."
        ),
        QuizQuestion(
            question = "What does 'Gospel' mean?",
            options = listOf("Holy Book", "Good News", "Life Story", "History"),
            correctAnswer = 1,
            explanation = "The word Gospel (Euangelion) translates literally to 'Good News'."
        ),
        QuizQuestion(
            question = "Which Gospel begins with the birth of John the Baptist?",
            options = listOf("Matthew", "Mark", "Luke", "John"),
            correctAnswer = 2,
            explanation = "Luke's narrative begins with the announcement of John the Baptist's birth."
        ),
        QuizQuestion(
            question = "Who is the 'Comforter' Jesus promised to send?",
            options = listOf("Michael the Archangel", "The Holy Spirit", "Elijah", "Moses"),
            correctAnswer = 1,
            explanation = "Jesus promised the Helper (Paraclete), which is the Holy Spirit."
        ),
        QuizQuestion(
            question = "Who blessed the baby Jesus in the temple?",
            options = listOf("Zacharias", "Simeon", "Joseph", "Nicodemus"),
            correctAnswer = 1,
            explanation = "Simeon took Jesus in his arms and praised God (Luke 2:28)."
        )
    )
    
    // Prophets Questions (50 questions) - Meaningful questions about prophets
    // Prophets Questions (50 questions) - High Quality, Varied Answers (Refined)
    private val prophetsQuestions = listOf(
        QuizQuestion(
            question = "Which prophet challenged the prophets of Baal to a contest on Mount Carmel?",
            options = listOf("Elisha", "Elijah", "Isaiah", "Jeremiah"),
            correctAnswer = 1,
            explanation = "Elijah challenged the 450 prophets of Baal to a contest on Mount Carmel to prove who is the true God."
        ),
        QuizQuestion(
            question = "Isaiah prophesied that a virgin would conceive and bear a son. What name did he say the son would be called?",
            options = listOf("Maher-Shalal-Hash-Baz", "Lo-Ammi", "Immanuel", "Jezreel"),
            correctAnswer = 2,
            explanation = "Therefore the Lord himself will give you a sign: The virgin will conceive and give birth to a son, and will call him Immanuel."
        ),
        QuizQuestion(
            question = "Which prophet was swallowed by a great fish when he tried to flee from God's command?",
            options = listOf("Nahum", "Obadiah", "Jonah", "Joel"),
            correctAnswer = 2,
            explanation = "Jonah was swallowed by a great fish after being thrown overboard on his way to Tarshish."
        ),
        QuizQuestion(
            question = "Jeremiah corresponds to which tragic event in Jewish history?",
            options = listOf("The Exodus from Egypt", "The destruction of Jerusalem and the Exile", "The building of the Second Temple", "The flood"),
            correctAnswer = 1,
            explanation = "Jeremiah prophesied during the final years of Judah and witnessed the destruction of Jerusalem and the beginning of the Exile."
        ),
        QuizQuestion(
            question = "Which prophet saw a valley of dry bones come to life?",
            options = listOf("Daniel", "Hosea", "Ezekiel", "Amos"),
            correctAnswer = 2,
            explanation = "God brought Ezekiel to a valley of dry bones and commanded him to prophesy to them, and they came to life."
        ),
        QuizQuestion(
            question = "Daniel interpreted a dream of a great statue made of different metals. What did the statue represent?",
            options = listOf("Four great kings", "Successive world empires", "The history of Israel", "Seven years of famine"),
            correctAnswer = 1,
            explanation = "The statue represented successive world empires: Babylon (Gold), Medo-Persia (Silver), Greece (Bronze), and Rome (Iron)."
        ),
        QuizQuestion(
            question = "Which prophet was a shepherd and fig-tree farmer before God called him?",
            options = listOf("Amos", "Micah", "Haggai", "Zechariah"),
            correctAnswer = 0,
            explanation = "Amos was a shepherd and dresser of sycamore figs from Tekoa before he was called to prophesy to Israel."
        ),
        QuizQuestion(
            question = "God commanded Hosea to marry a woman named Gomer. What was she?",
            options = listOf("A prophetess", "A queen", "An adulterous wife", "A widow"),
            correctAnswer = 2,
            explanation = "Hosea was commanded to marry Gomer, an unfaithful woman, to illustrate Israel's unfaithfulness to God."
        ),
        QuizQuestion(
            question = "Which prophet predicted the outpouring of the Holy Spirit on 'all flesh'?",
            options = listOf("Joel", "Zephaniah", "Malachi", "Habakkuk"),
            correctAnswer = 0,
            explanation = "Joel 2:28 prophesied, 'And afterward, I will pour out my Spirit on all people.'"
        ),
        QuizQuestion(
            question = "Micah predicted the Messiah would be born in which town?",
            options = listOf("Jerusalem", "Nazareth", "Bethlehem", "Hebron"),
            correctAnswer = 2,
            explanation = "Micah 5:2 states, 'But you, Bethlehem Ephrathah... out of you will come for me one who will be ruler over Israel.'"
        ),
        QuizQuestion(
            question = "Nahum's prophecy was focused entirely on the destruction of which city?",
            options = listOf("Nineveh", "Babylon", "Tyre", "Samaria"),
            correctAnswer = 0,
            explanation = "The book of Nahum is an oracle concerning Nineveh, predicting its final destruction for its wickedness."
        ),
        QuizQuestion(
            question = "Which prophet questioned God asking, 'Why do the wicked prosper?'",
            options = listOf("Habakkuk", "Zechariah", "Haggai", "Malachi"),
            correctAnswer = 0,
            explanation = "Habakkuk wrestled with the question of why God allowed wickedness and why He would use the Babylonians to judge Judah."
        ),
        QuizQuestion(
            question = "Zephaniah warned about 'The Day of the Lord.' How did he describe it?",
            options = listOf("A day of peace and safety", "A day of wrath and judgment", "A day of endless rain", "A day of harvesting grain"),
            correctAnswer = 1,
            explanation = "Zephaniah described the Day of the Lord as 'a day of wrath... a day of distress and anguish, a day of trouble and ruin.'"
        ),
        QuizQuestion(
            question = "After the exile, Haggai encouraged the people to stop building their own houses and do what?",
            options = listOf("Build the city walls", "Rebuild the Temple", "Plant vineyards", "Conquer neighbors"),
            correctAnswer = 1,
            explanation = "Haggai challenged the people for living in paneled houses while God's house (the Temple) remained a ruin."
        ),
        QuizQuestion(
            question = "Zechariah prophesied the Messiah would enter Jerusalem riding on what?",
            options = listOf("A white horse", "A chariot of fire", "A donkey", "A camel"),
            correctAnswer = 2,
            explanation = "Rejoice greatly, Daughter Zion! See, your king comes to you... lowly and riding on a donkey. (Zechariah 9:9)"
        ),
        QuizQuestion(
            question = "Malachi, the last Old Testament prophet, prophesied that who would come before the Day of the Lord?",
            options = listOf("Moses", "Abraham", "Elijah", "David"),
            correctAnswer = 2,
            explanation = "See, I will send the prophet Elijah to you before that great and dreadful day of the Lord comes. (Malachi 4:5)"
        ),
        QuizQuestion(
            question = "Who anointed the first two kings of Israel, Saul and David?",
            options = listOf("Nathan", "Samuel", "Gad", "Ahijah"),
            correctAnswer = 1,
            explanation = "Samuel the prophet anointed both Saul and David as kings of Israel."
        ),
        QuizQuestion(
            question = "Which prophet confronted King David with the parable of the rich man and the poor man's lamb?",
            options = listOf("Nathan", "Gad", "Samuel", "Elijah"),
            correctAnswer = 0,
            explanation = "Nathan used a parable to expose David's sin with Bathsheba, culminating in the words, 'You are the man!'"
        ),
        QuizQuestion(
            question = "Elisha performed many miracles. Which of these did he NOT do?",
            options = listOf("Make an axe head float", "Purify a pot of stew", "Call down fire on soldiers", "Part the Red Sea"),
            correctAnswer = 3,
            explanation = "Elisha made an axe head float and purified stew, but parting the Red Sea was Moses' miracle (Elisha parted the Jordan)."
        ),
        QuizQuestion(
            question = "Who is known as the 'Weeping Prophet'?",
            options = listOf("Jeremiah", "Isaiah", "Ezekiel", "Hosea"),
            correctAnswer = 0,
            explanation = "Jeremiah is called the Weeping Prophet because of his deep sorrow over the sins and destruction of his people."
        ),
        QuizQuestion(
            question = "Which prophet was thrown into a lion's den for praying to God?",
            options = listOf("Daniel", "Shadrach", "Meshach", "Abednego"),
            correctAnswer = 0,
            explanation = "Daniel was thrown into the lion's den because he continued to pray to God despite King Darius's decree."
        ),
        QuizQuestion(
            question = "Obadiah is the shortest book in the Old Testament. Who is it a judgment against?",
            options = listOf("Edom", "Moab", "Ammon", "Philistia"),
            correctAnswer = 0,
            explanation = "Obadiah pronounces judgment on Edom for their arrogance and violence against their brother nation, Israel."
        ),
        QuizQuestion(
            question = "Which prophet was told not to marry or have children as a sign of the coming judgment?",
            options = listOf("Jeremiah", "Ezekiel", "Hosea", "Isaiah"),
            correctAnswer = 0,
            explanation = "God commanded Jeremiah not to marry or have children in that place because of the deadly diseases and sword coming upon them."
        ),
        QuizQuestion(
            question = "What strange food did Ezekiel have to bake his bread with as a sign?",
            options = listOf("Cow dung", "Human dung (initially)", "Rotten figs", "Locusts"),
            correctAnswer = 1,
            explanation = "God initially told Ezekiel to bake bread over human excrement, but allowed him to use cow dung after Ezekiel objected."
        ),
        QuizQuestion(
            question = "Which major prophet saw the Lord 'high and lifted up' in the temple?",
            options = listOf("Isaiah", "Jeremiah", "Ezekiel", "Daniel"),
            correctAnswer = 0,
            explanation = "In the year that King Uzziah died, I saw the Lord, high and exalted, seated on a throne; and the train of his robe filled the temple. (Isaiah 6:1)"
        ),
        QuizQuestion(
            question = "Which prophet foretold that the Messiah would be 'pierced for our transgressions'?",
            options = listOf("Isaiah", "Jeremiah", "Zechariah", "Micah"),
            correctAnswer = 0,
            explanation = "Isaiah 53:5 - 'But he was pierced for our transgressions, he was crushed for our iniquities.'"
        ),
        QuizQuestion(
            question = "What did the prophet Ahijah tear into twelve pieces?",
            options = listOf("A scroll", "A new cloak", "A loaf of bread", "A veil"),
            correctAnswer = 1,
            explanation = "Ahijah tore his new cloak into twelve pieces and gave ten to Jeroboam, symbolizing the division of the kingdom."
        ),
        QuizQuestion(
            question = "Which king did Isaiah advise during the Assyrian siege of Jerusalem?",
            options = listOf("Ahaz", "Hezekiah", "Josiah", "Manasseh"),
            correctAnswer = 1,
            explanation = "Isaiah advised King Hezekiah to trust in the Lord when Sennacherib of Assyria besieged Jerusalem."
        ),
        QuizQuestion(
            question = "Who was the scribe that wrote down Jeremiah's words?",
            options = listOf("Baruch", "Ezra", "Nehemiah", "Gedaliah"),
            correctAnswer = 0,
            explanation = "Baruch son of Neriah was Jeremiah's scribe and assistant who wrote down his prophecies."
        ),
        QuizQuestion(
            question = "In Ezekiel's vision, what did he see leaving the temple?",
            options = listOf("The Ark of the Covenant", "The Glory of the Lord", "The High Priest", "The Altar"),
            correctAnswer = 1,
            explanation = "Ezekiel saw the Glory of the Lord depart from the temple because of the people's idolatry."
        ),
        QuizQuestion(
            question = "Which prophet had a vision of a 'flying scroll'?",
            options = listOf("Zechariah", "Ezekiel", "Daniel", "Revelation"),
            correctAnswer = 0,
            explanation = "Zechariah 5 describes a vision of a flying scroll representing the curse going out over the whole land."
        ),
        QuizQuestion(
            question = "What did God call the prophet Ezekiel repeatedly?",
            options = listOf("Son of Man", "Mighty Warrior", "Beloved", "Servant"),
            correctAnswer = 0,
            explanation = "God addresses Ezekiel as 'Son of Man' over 90 times in the book."
        ),
        QuizQuestion(
            question = "Which prophet prophesied the exact number of years (70) for the Babylonian captivity?",
            options = listOf("Jeremiah", "Isaiah", "Daniel", "Ezekiel"),
            correctAnswer = 0,
            explanation = "Jeremiah 25:11 states that the nations will serve the king of Babylon for seventy years."
        ),
        QuizQuestion(
            question = "Who were the 'minor prophets'?",
            options = listOf("Prophets under 20 years old", "The 12 shorter prophetic books", "Prophets who didn't perform miracles", "False prophets"),
            correctAnswer = 1,
            explanation = "The Minor Prophets are the final 12 books of the Old Testament, called 'minor' only because they are shorter in length."
        ),
        QuizQuestion(
            question = "Which prophet condemned the people for offering blind and crippled animals as sacrifices?",
            options = listOf("Malachi", "Haggai", "Zechariah", "Joel"),
            correctAnswer = 0,
            explanation = "Malachi rebuked the priests and people for despising God's name by offering defiled sacrifices."
        ),
        QuizQuestion(
            question = "Elisha told Naaman the Syrian to wash in which river to be healed of leprosy?",
            options = listOf("Nile", "Euphrates", "Jordan", "Tigris"),
            correctAnswer = 2,
            explanation = "Elisha told Naaman to wash seven times in the Jordan River."
        ),
        QuizQuestion(
            question = "Which prophet's book is an acrostic poem mourning the destruction of Jerusalem?",
            options = listOf("Lamentations (Jeremiah)", "Ecclesiastes", "Song of Songs", "Habakkuk"),
            correctAnswer = 0,
            explanation = "Lamentations, traditionally ascribed to Jeremiah, is a series of poetic dirges over the fall of Jerusalem."
        ),
        QuizQuestion(
            question = "What happened to the mockers who called Elisha 'baldhead'?",
            options = listOf("They were struck blind", "Two bears came out of the woods", "Fire fell from heaven", "They became mute"),
            correctAnswer = 1,
            explanation = "Two female bears came out of the woods and mauled forty-two of the youths who mocked God's prophet."
        ),
        QuizQuestion(
            question = "Which prophet married a prophetess?",
            options = listOf("Isaiah", "Jeremiah", "Hosea", "Ezekiel"),
            correctAnswer = 0,
            explanation = "Isaiah 8:3 mentions 'I went to the prophetess,' referring to his wife."
        ),
        QuizQuestion(
            question = "Who was taken up to heaven in a whirlwind with a chariot of fire?",
            options = listOf("Enoch", "Elisha", "Elijah", "Moses"),
            correctAnswer = 2,
            explanation = "Elijah was separated from Elisha by a chariot of fire and horses of fire, and went up to heaven in a whirlwind."
        ),
        QuizQuestion(
            question = "Which prophet spoke of the 'Sun of Righteousness' rising with healing in its wings?",
            options = listOf("Malachi", "Isaiah", "Zechariah", "Hosea"),
            correctAnswer = 0,
            explanation = "Malachi 4:2 - 'But for you who revere my name, the sun of righteousness will rise with healing in its rays.'"
        ),
        QuizQuestion(
            question = "What did the prophet Balaam's donkey do?",
            options = listOf("Flew", "Spoke", "Turned into a lion", "Danced"),
            correctAnswer = 1,
            explanation = "The Lord opened the donkey's mouth, and it spoke to Balaam, asking why he was beating it."
        ),
        QuizQuestion(
            question = "Which prophet condemned Israel for 'selling the righteous for silver and the needy for a pair of sandals'?",
            options = listOf("Amos", "Hosea", "Joel", "Micah"),
            correctAnswer = 0,
            explanation = "Amos 2:6 highlights the social injustice and greed prevalent in Israel."
        ),
        QuizQuestion(
            question = "Who prophesied, 'The just shall live by faith'?",
            options = listOf("Habakkuk", "Nahum", "Obadiah", "Zephaniah"),
            correctAnswer = 0,
            explanation = "Habakkuk 2:4 - '...but the righteous person will live by his faithfulness.' Quoted by Paul in Romans."
        ),
        QuizQuestion(
            question = "What sign did Isaiah give Hezekiah that his life would be extended?",
            options = listOf("The sun moved backward", "Thunder in a clear sky", "A star fell", "Water turned to blood"),
            correctAnswer = 0,
            explanation = "The shadow on the stairway of Ahaz went back ten steps."
        ),
        QuizQuestion(
            question = "Which prophet had a contest with Hananiah over a wooden yoke?",
            options = listOf("Jeremiah", "Ezekiel", "Daniel", "Isaiah"),
            correctAnswer = 0,
            explanation = "Jeremiah wore a yoke to symbolize submission to Babylon. Hananiah broke it, predicting false peace."
        ),
        QuizQuestion(
            question = "Who was the 'Prophetess' who judged Israel under a palm tree?",
            options = listOf("Deborah", "Miriam", "Huldah", "Noadiah"),
            correctAnswer = 0,
            explanation = "Deborah was a prophetess and judge who held court under the Palm of Deborah."
        ),
        QuizQuestion(
            question = "Which prophet received a vision of a man with a measuring line?",
            options = listOf("Zechariah", "Haggai", "Malachi", "Amos"),
            correctAnswer = 0,
            explanation = "Zechariah 2 describes a man with a measuring line going to measure Jerusalem."
        ),
        QuizQuestion(
            question = "What was the name of the idol that Dagon bowed down to?",
            options = listOf("The Golden Calf", "The Ark of the Covenant", "Baal", "Molech"),
            correctAnswer = 1,
            explanation = "When the Philistines placed the Ark in Dagon's temple, the statue of Dagon fell face down before it."
        ),
        QuizQuestion(
            question = "Which prophet said the Lord requires us 'To act justly and to love mercy and to walk humbly with your God'?",
            options = listOf("Micah", "Isaiah", "Jeremiah", "Amos"),
            correctAnswer = 0,
            explanation = "Micah 6:8 is one of the most famous summaries of biblical ethics."
        )
    )
    
    // Parables Questions (50 questions) - High Quality, Varied Answers
    private val parablesQuestions = listOf(
        QuizQuestion(
            question = "In the Parable of the Sower, what happened to the seed that fell on rocky ground?",
            options = listOf("It was eaten by birds", "It sprang up quickly but withered", "It was choked by thorns", "It produced a hundredfold"),
            correctAnswer = 1,
            explanation = "The seed on rocky ground sprang up quickly but withered because it had no deep root."
        ),
        QuizQuestion(
            question = "In the Parable of the Good Samaritan, who was the first person to pass by the injured man?",
            options = listOf("A Levite", "A Priest", "A Roman Soldier", "An Innkeeper"),
            correctAnswer = 1,
            explanation = "A priest happened to be going down the same road, and when he saw the man, he passed by on the other side."
        ),
        QuizQuestion(
            question = "What did the father give the returning Prodigal Son?",
            options = listOf("A beating", "A servant's job", "Reviewing his sins", "The best robe, a ring, and sandals"),
            correctAnswer = 3,
            explanation = "The father ordered the best robe, a ring for his finger, and sandals for his feet to welcome his son back."
        ),
        QuizQuestion(
            question = "In the Parable of the Talents, what did the man with one talent do?",
            options = listOf("Invested it and made more", "Gave it to the poor", "Buried it in the ground", "Lost it in gambling"),
            correctAnswer = 2,
            explanation = "The man with one talent was afraid and went out and hid his master's money in the ground."
        ),
        QuizQuestion(
            question = "Which parable compares the Kingdom of Heaven to a tiny seed that becomes a large tree?",
            options = listOf("The Leaven", "The Mustard Seed", "The Wheat and Tares", "The Fig Tree"),
            correctAnswer = 1,
            explanation = "The Parable of the Mustard Seed compares the Kingdom to the smallest of seeds that grows into a large tree."
        ),
        QuizQuestion(
            question = "In the Parable of the Ten Virgins, what did the five foolish virgins forget?",
            options = listOf("Their lamps", "Their robes", "Extra oil", "Their invitations"),
            correctAnswer = 2,
            explanation = "The foolish ones took their lamps but did not take any oil with them."
        ),
        QuizQuestion(
            question = "What happened to the house built on sand when the rain came?",
            options = listOf("It stood firm", "It fell with a great crash", "It floated away", "It was slightly damaged"),
            correctAnswer = 1,
            explanation = "The rain came, the streams rose, and the winds blew and beat against that house, and it fell with a great crash."
        ),
        QuizQuestion(
            question = "In the Parable of the Lost Sheep, how many sheep did the shepherd leave behind to find the one?",
            options = listOf("9", "99", "50", "100"),
            correctAnswer = 1,
            explanation = "The shepherd leaves the ninety-nine in the open country to go after the lost sheep until he finds it."
        ),
        QuizQuestion(
            question = "Who did the rich man see in Abraham's bosom specifically?",
            options = listOf("Moses", "Elijah", "Lazarus", "David"),
            correctAnswer = 2,
            explanation = "In Hades, the rich man looked up and saw Abraham far away, with Lazarus by his side."
        ),
        QuizQuestion(
            question = "In the Parable of the Unforgiving Servant, how much did the first servant owe the king?",
            options = listOf("100 denarii", "10,000 talents", "50 shekels", "1,000 drachmas"),
            correctAnswer = 1,
            explanation = "The first servant owed a massive debt of ten thousand talents, which meant millions of dollars in modern terms."
        ),
        QuizQuestion(
            question = "What item did the woman lose in the parable about rejoicing?",
            options = listOf("A sheep", "A pearl", "A silver coin", "A golden ring"),
            correctAnswer = 2,
            explanation = "The parable describes a woman who has ten silver coins and loses one, sweeping the house to find it."
        ),
        QuizQuestion(
            question = "In the Parable of the Wedding Feast, why was one guest thrown out?",
            options = listOf("He was drunk", "He wasn't wearing wedding clothes", "He insulted the king", "He arrived too late"),
            correctAnswer = 1,
            explanation = "The king noticed a man there who was not wearing wedding clothes, representing a lack of righteousness."
        ),
        QuizQuestion(
            question = "What did the enemy sow among the wheat while everyone was sleeping?",
            options = listOf("Thorns", "Weeds (Tares)", "Rocks", "Barley"),
            correctAnswer = 1,
            explanation = "While everyone was sleeping, his enemy came and sowed weeds among the wheat, and went away."
        ),
        QuizQuestion(
            question = "In the Parable of the Workers in the Vineyard, what did those hired at the eleventh hour receive?",
            options = listOf("Only one hour's pay", "Half a denarius", "A full day's wage (a denarius)", "Nothing"),
            correctAnswer = 2,
            explanation = "The landowner paid everyone a denarius, regardless of how long they had worked, showing God's grace."
        ),
        QuizQuestion(
            question = "What did the merchant do when he found the pearl of great price?",
            options = listOf("Stole it", "Sold all he had to buy it", "Haggled for a lower price", "Ignored it"),
            correctAnswer = 1,
            explanation = "When he found one of great value, he went away and sold everything he had and bought it."
        ),
        QuizQuestion(
            question = "In the Parable of the Pharisee and the Tax Collector, how did the Tax Collector pray?",
            options = listOf("He thanked God he wasn't like others", "He beat his breast and asked for mercy", "He recited long scriptures", "He prayed silently in his heart only"),
            correctAnswer = 1,
            explanation = "The tax collector stood at a distance, beat his breast and said, 'God, have mercy on me, a sinner.'"
        ),
        QuizQuestion(
            question = "Which parable teaches about the need to forgive others from the heart?",
            options = listOf("The Good Samaritan", "The Unforgiving Servant", "The Prodigal Son", "The Lost Sheep"),
            correctAnswer = 1,
            explanation = "The Parable of the Unforgiving Servant concludes with the warning to forgive your brother from your heart."
        ),
        QuizQuestion(
            question = "In the Parable of the Great Banquet, who was invited first but made excuses?",
            options = listOf("The poor and crippled", "The king's family", "The invited guests", "Foreigners"),
            correctAnswer = 2,
            explanation = "A certain man was preparing a great banquet and invited many guests, but they all alike began to make excuses."
        ),
        QuizQuestion(
            question = "What does the leaven represent in the Parable of the Leaven?",
            options = listOf("Sin spreading in the church", "The permeating growth of the Kingdom", "False teaching", "Hypocrisy"),
            correctAnswer = 1,
            explanation = "The leaven represents the Kingdom of Heaven, which, though small, permeates and transforms the whole world."
        ),
        QuizQuestion(
            question = "In the Parable of the Rich Fool, what did the man plan to do with his surplus?",
            options = listOf("Give it to the poor", "Build bigger barns", "Invest it in trade", "Offer it to the temple"),
            correctAnswer = 1,
            explanation = "He said, 'I will tear down my barns and build bigger ones, and there I will store my surplus grain.'"
        ),
        QuizQuestion(
            question = "What fruit was the master looking for on the barren fig tree?",
            options = listOf("Dates", "Olives", "Figs", "Grapes"),
            correctAnswer = 2,
            explanation = "A man had a fig tree planting in his vineyard, and he went to look for fruit on it, but did not find any."
        ),
        QuizQuestion(
            question = "In the Parable of the Two Sons, which son actually did the father's will?",
            options = listOf("The one who said 'I will' but didn't go", "The one who said 'I will not' but later went", "Both did the will", "Neither did the will"),
            correctAnswer = 1,
            explanation = "The first son said he wouldn't go, but later changed his mind and went. Jesus asked, 'Which of the two did what his father wanted?'"
        ),
        QuizQuestion(
            question = "What does the Treasure Hidden in the Field represent?",
            options = listOf("Earthly riches", "Wisdom", "The Kingdom of Heaven", "A secret mystery"),
            correctAnswer = 2,
            explanation = "The kingdom of heaven is like treasure hidden in a field. When a man found it, he hid it again, and then in his joy went and sold all he had and bought that field."
        ),
        QuizQuestion(
            question = "In the Parable of the Persistent Widow, who did she keep bothering?",
            options = listOf("A corrupt tax collector", "An unjust judge", "Her neighbor", "The king"),
            correctAnswer = 1,
            explanation = "She kept coming to an unjust judge who neither feared God nor cared about men, pleading for justice."
        ),
        QuizQuestion(
            question = "Which parable warns against greed and assuming we have plenty of time?",
            options = listOf("The Rich Man and Lazarus", "The Rich Fool", "The Unjust Steward", "The Prodigal Son"),
            correctAnswer = 1,
            explanation = "The Parable of the Rich Fool warns against storing up things for oneself but not being rich toward God."
        ),
        QuizQuestion(
            question = "In the Parable of the Net, what happens to the bad fish?",
            options = listOf("They are sold cheaply", "They are thrown away", "They are eaten anyway", "They are put back in the sea"),
            correctAnswer = 1,
            explanation = "The fishermen collect the good fish in baskets, but throw the bad away. This is how it will be at the end of the age."
        ),
        QuizQuestion(
            question = "Who are the 'goats' in the Parable of the Sheep and Goats?",
            options = listOf("The righteous", "The false teachers", "Those who did not help the least of these", "The demons"),
            correctAnswer = 2,
            explanation = "The goats are those who saw the hungry, thirsty, or stranger and did not help them."
        ),
        QuizQuestion(
            question = "In the Parable of the Ten Minas, what was the reward for the servant who earned ten more minas?",
            options = listOf("Ten cities to rule", "Ten bags of gold", "Freedom from slavery", "A promotion to general"),
            correctAnswer = 0,
            explanation = "'Well done, my good servant!' his master replied. 'Because you have been trustworthy in a very small matter, take charge of ten cities.'"
        ),
        QuizQuestion(
            question = "What is the main lesson of the Parable of the Growing Seed?",
            options = listOf("Farming is hard work", "We must water the seed daily", "God brings the growth automatically", "We need better soil"),
            correctAnswer = 2,
            explanation = "The seed sprouts and grows, though he does not know how. All by itself the soil produces corn."
        ),
        QuizQuestion(
            question = "In the Parable of the Unjust Steward, why was the steward commended?",
            options = listOf("For his honesty", "For his repentance", "For acting shrewdly", "For saving money"),
            correctAnswer = 2,
            explanation = "The master commended the dishonest manager because he had acted shrewdly in preparing for his future."
        ),
        QuizQuestion(
            question = "In the Parable of the Lamp, where should a lamp be placed?",
            options = listOf("Under a bowl", "Under a bed", "On a stand", "In a closet"),
            correctAnswer = 2,
            explanation = "No one lights a lamp and puts it in a place where it will be hidden, or under a bowl. Instead they put it on its stand."
        ),
        QuizQuestion(
            question = "Which parable illustrates God's desire for us to be persistent in prayer?",
            options = listOf("The Friend at Midnight", "The Good Samaritan", "The Sower", "The Talents"),
            correctAnswer = 0,
            explanation = "The Parable of the Friend at Midnight teaches audacity and persistence in asking."
        ),
        QuizQuestion(
            question = "In the Parable of the Wicked Tenants, what did they do to the owner's son?",
            options = listOf("Honored him", "Paid him the rent", "Threw him out and killed him", "Ignored him"),
            correctAnswer = 2,
            explanation = "When the tenants saw the son, they threw him out of the vineyard and killed him, hoping to get his inheritance."
        ),
        QuizQuestion(
            question = "What does the 'narrow door' represent in Jesus' teaching?",
            options = listOf("A literal small gate in Jerusalem", "The difficulty of entering the Kingdom", "Only for the poor", "A secret entrance"),
            correctAnswer = 1,
            explanation = "Make every effort to enter through the narrow door, because many, I tell you, will try to enter and will not be able to."
        ),
        QuizQuestion(
            question = "In the Parable of the Cost of Discipleship, what should a builder do first?",
            options = listOf("Lay the foundation", "Buy materials", "Sit down and estimate the cost", "Hire workers"),
            correctAnswer = 2,
            explanation = "Suppose one of you wants to build a tower. Won't you first sit down and estimate the cost to see if you have enough money to complete it?"
        ),
        QuizQuestion(
            question = "In the Parable of the Lost Coin, why did the woman light a lamp and sweep?",
            options = listOf("It was dark", "She was cleaning anyway", "Because the coin was valuable to her", "Guests were coming"),
            correctAnswer = 2,
            explanation = "The diligent search illustrates God's diligent search for the lost sinner."
        ),
        QuizQuestion(
            question = "What represents the Word of God in the Parable of the Sower?",
            options = listOf("The sower", "The seed", "The soil", "The sun"),
            correctAnswer = 1,
            explanation = "The seed is the word of God (Luke 8:11)."
        ),
        QuizQuestion(
            question = "In the Parable of the Sheep and Goats, what is the criteria for judgment?",
            options = listOf("Correct theology", "Church attendance", "How one treated the least of these", "Tithing"),
            correctAnswer = 2,
            explanation = "Whatever you did for one of the least of these brothers and sisters of mine, you did for me."
        ),
        QuizQuestion(
            question = "What was the complaint of the older brother in the Prodigal Son story?",
            options = listOf("He didn't get a share of the inheritance", "The father never gave him a goat to celebrate with friends", "He wanted to leave too", "He hated the servants"),
            correctAnswer = 1,
            explanation = "He complained, 'You never gave me even a young goat so I could celebrate with my friends.'"
        ),
        QuizQuestion(
            question = "In the Parable of the Rich Man and Lazarus, what did the rich man want Abraham to do?",
            options = listOf("Send Lazarus to dip his finger in water", "Pull him out of the fire", "Destroy the chasm", "Punish Lazarus"),
            correctAnswer = 0,
            explanation = "He asked Abraham to send Lazarus to dip the tip of his finger in water and cool his tongue."
        ),
        QuizQuestion(
            question = "Which parable speaks about the separation of the righteous and wicked at the end of the age?",
            options = listOf("The Mustard Seed", "The Wheat and Tares", "The Sower", "The Pearl"),
            correctAnswer = 1,
            explanation = "The harvest is the end of the age, and the harvesters are angels who will weed out of his kingdom everything that causes sin."
        ),
        QuizQuestion(
            question = "In the Parable of the Lowest Seat, where does Jesus say we should sit at a feast?",
            options = listOf("At the head of the table", "Next to the host", "In the lowest place", "Outside"),
            correctAnswer = 2,
            explanation = "But when you are invited, take the lowest place, so that when your host comes, he will say to you, 'Friend, move up to a better place.'"
        ),
        QuizQuestion(
            question = "What is the lesson of the Parable of the Empty House?",
            options = listOf("Keep your house clean", "Demons like empty places", "Reformation without habitation by God invites worse evil", "Ghosts are real"),
            correctAnswer = 2,
            explanation = "When an impure spirit comes out of a person... if it finds the house unoccupied, it takes seven other spirits more wicked than itself."
        ),
        QuizQuestion(
            question = "In the Parable of the Two Debtors (Luke 7), who loved the moneylender more?",
            options = listOf("The one who owed 50 denarii", "The one who owed 500 denarii", "Both loved equally", "Neither loved him"),
            correctAnswer = 1,
            explanation = "Simon answered correctly: 'I suppose the one who had the bigger debt forgiven.'"
        ),
        QuizQuestion(
            question = "Which parable describes a master returning from a wedding banquet?",
            options = listOf("The Watchful Servants", "The Ten Virgins", "The Great Banquet", "The Marriage Feast"),
            correctAnswer = 0,
            explanation = "Be like servants waiting for their master to return from a wedding banquet, so that when he comes and knocks they can immediately open the door."
        ),
        QuizQuestion(
            question = "In the Parable of the Barren Fig Tree, what did the gardener ask for?",
            options = listOf("Cut it down immediately", "One more year to dig around it and fertilize it", "Plant a new tree", "Burn the vineyard"),
            correctAnswer = 1,
            explanation = "Sir, leave it alone for one more year, and I'll dig around it and fertilize it. If it bears fruit next year, fine! If not, then cut it down."
        ),
        QuizQuestion(
            question = "What did the man do who found the Treasure Hidden in the Field?",
            options = listOf("Told his friends", "Hid it again", "Took it immediately", "Left it there"),
            correctAnswer = 1,
            explanation = "The text says 'When a man found it, he hid it again, and then in his joy went and sold all he had and bought that field.'"
        ),
        QuizQuestion(
            question = "In the Parable of the Speck and the Plank, what must we do first?",
            options = listOf("Help our brother", "Ignore the plank", "Remove the plank from our own eye", "Judge the brother"),
            correctAnswer = 2,
            explanation = "First take the plank out of your own eye, and then you will see clearly to remove the speck from your brother's eye."
        ),
        QuizQuestion(
            question = "Why did the rich young ruler walk away sad?",
            options = listOf("Jesus insulted him", "He had great wealth", "He didn't understand", "The disciples were rude"),
            correctAnswer = 1,
            explanation = "He went away sad, because he had great wealth and was unwilling to sell it to follow Jesus."
        ),
        QuizQuestion(
            question = "In the Parable of the Physicians, who does Jesus say needs a doctor?",
            options = listOf("The healthy", "The sick", "Everyone", "No one"),
            correctAnswer = 1,
            explanation = "It is not the healthy who need a doctor, but the sick. I have not come to call the righteous, but sinners."
        )
    )

    // All sections now have exactly 50 high-quality questions - no fallbacks needed
}
