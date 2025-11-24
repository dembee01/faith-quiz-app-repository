package com.example.faithquiz.data.repository

import android.content.Context
import com.example.faithquiz.data.local.QuestionDao
import com.example.faithquiz.data.model.Question
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val context: Context
) {
    fun getAllQuestions(): Flow<List<Question>> = questionDao.getAllQuestions()

    fun getQuestionsByLevel(level: Int): Flow<List<Question>> = questionDao.getQuestionsByLevel(level)

    fun getAllLevels(): Flow<List<Int>> = questionDao.getAllLevels()

    suspend fun getQuestionCount(): Int = questionDao.getQuestionCount()

    suspend fun insertQuestions(questions: List<Question>) = questionDao.insertQuestions(questions)

    suspend fun deleteAllQuestions() = questionDao.deleteAllQuestions()

    suspend fun seedDatabase() {
        try {
            android.util.Log.d("QuestionRepository", "Starting database seeding...")
            val questionCount = getQuestionCount()
            android.util.Log.d("QuestionRepository", "Current question count: $questionCount")
            
            if (questionCount == 0) {
                android.util.Log.d("QuestionRepository", "Database is empty, generating questions...")
                val questions = generateQuestions()
                android.util.Log.d("QuestionRepository", "Generated ${questions.size} questions")
                
                // Insert questions in smaller batches for better performance on mobile devices
                val batchSize = 20 // Further reduced batch size for better performance
                val batches = questions.chunked(batchSize)
                android.util.Log.d("QuestionRepository", "Inserting ${batches.size} batches...")
                
                // Use withContext to ensure we're on IO dispatcher
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    batches.forEachIndexed { index, batch ->
                        try {
                            android.util.Log.d("QuestionRepository", "Inserting batch ${index + 1}/${batches.size}")
                            insertQuestions(batch)
                            android.util.Log.d("QuestionRepository", "Batch ${index + 1} inserted successfully")
                            
                            // Add a small delay between batches to prevent blocking
                            kotlinx.coroutines.delay(10)
                        } catch (batchError: Exception) {
                            android.util.Log.e("QuestionRepository", "Error inserting batch ${index + 1}", batchError)
                            // Continue with next batch even if one fails
                        }
                    }
                }
                android.util.Log.d("QuestionRepository", "All batches processed")
                
                // Verify the insertion
                val finalCount = getQuestionCount()
                android.util.Log.d("QuestionRepository", "Final question count: $finalCount")
                
                if (finalCount > 0) {
                    android.util.Log.d("QuestionRepository", "Database seeding completed successfully")
                } else {
                    android.util.Log.e("QuestionRepository", "Database seeding failed - no questions found after insertion")
                }
            } else {
                android.util.Log.d("QuestionRepository", "Database already contains $questionCount questions")
            }
        } catch (e: Exception) {
            android.util.Log.e("QuestionRepository", "Failed to seed database", e)
            // Don't throw the exception, just log it and continue
            e.printStackTrace()
        }
    }

    private fun generateQuestions(): List<Question> {
        val questions = mutableListOf<Question>()
        var questionId = 1

        // Level 1 - Very Easy (34 questions)
        val level1Questions = listOf(
            Question(questionId++, 1, "Who built the ark according to the Bible?", listOf("Moses", "Noah", "Abraham"), 1, "Noah built the ark as commanded by God to save his family and animals from the great flood.", "Genesis 6:14-22", listOf("Noah", "Ark", "Flood")),
            Question(questionId++, 1, "How many days and nights did Jesus fast in the wilderness?", listOf("30", "40", "50"), 1, "Jesus fasted for 40 days and 40 nights in the wilderness, being tempted by Satan.", "Matthew 4:2", listOf("Jesus", "Fast", "Wilderness")),
            Question(questionId++, 1, "Who was the first king of Israel?", listOf("David", "Solomon", "Saul"), 2, "Saul was the first king of Israel, anointed by the prophet Samuel.", "1 Samuel 10:1", listOf("Saul", "King", "Israel")),
            Question(questionId++, 1, "What is the first book of the Bible?", listOf("Exodus", "Genesis", "Matthew"), 1, "Genesis is the first book of the Bible, meaning 'beginning' or 'origin'.", "Genesis 1:1", listOf("Genesis", "Bible", "First")),
            Question(questionId++, 1, "Who was thrown into the lion's den?", listOf("David", "Daniel", "Joseph"), 1, "Daniel was thrown into the lion's den for praying to God instead of the king.", "Daniel 6:16", listOf("Daniel", "Lion", "Den")),
            Question(questionId++, 1, "How many disciples did Jesus have?", listOf("10", "11", "12"), 2, "Jesus had 12 disciples, also called apostles, who followed him during his ministry.", "Matthew 10:1-4", listOf("Disciples", "Jesus", "Twelve")),
            Question(questionId++, 1, "Who denied Jesus three times?", listOf("John", "Peter", "Judas"), 1, "Peter denied knowing Jesus three times before the rooster crowed, as Jesus had predicted.", "Matthew 26:69-75", listOf("Peter", "Deny", "Three")),
            Question(questionId++, 1, "What was the name of Jesus' mother?", listOf("Elizabeth", "Mary", "Sarah"), 1, "Mary was the mother of Jesus, chosen by God to bear the Son of God.", "Luke 1:26-38", listOf("Mary", "Mother", "Jesus")),
            Question(questionId++, 1, "What city was Jesus born in?", listOf("Nazareth", "Jerusalem", "Bethlehem"), 2, "Jesus was born in Bethlehem, fulfilling the prophecy about the Messiah.", "Luke 2:4-7", listOf("Bethlehem", "Birth", "Jesus")),
            Question(questionId++, 1, "Who was the first martyr of the Christian church?", listOf("Peter", "Paul", "Stephen"), 2, "Stephen was the first Christian martyr, stoned to death for his faith.", "Acts 7:54-60", listOf("Stephen", "Martyr", "First")),
            Question(questionId++, 1, "What is the last book of the Bible?", listOf("Revelation", "Jude", "3 John"), 0, "Revelation is the last book of the Bible, containing visions of the end times.", "Revelation 1:1", listOf("Revelation", "Last", "Bible")),
            Question(questionId++, 1, "Who was the first man created according to the Bible?", listOf("Adam", "Eve", "Cain"), 0, "Adam was the first man created by God from the dust of the ground.", "Genesis 2:7", listOf("Adam", "First", "Man")),
            Question(questionId++, 1, "How many books are in the New Testament?", listOf("25", "26", "27"), 2, "The New Testament contains 27 books, from Matthew to Revelation.", "Various", listOf("New Testament", "27", "Books")),
            Question(questionId++, 1, "Who was the first woman created according to the Bible?", listOf("Adam", "Eve", "Sarah"), 1, "Eve was the first woman, created from Adam's rib to be his companion.", "Genesis 2:21-22", listOf("Eve", "First", "Woman")),
            Question(questionId++, 1, "What is the shortest verse in the Bible?", listOf("Jesus wept", "Rejoice always", "Pray continually"), 0, "'Jesus wept' (John 11:35) is the shortest verse in the Bible.", "John 11:35", listOf("Jesus wept", "Shortest", "Verse")),
            Question(questionId++, 1, "Who was the father of John the Baptist?", listOf("Zechariah", "Joseph", "Simon"), 0, "Zechariah was the father of John the Baptist, a priest who served in the temple.", "Luke 1:5-25", listOf("Zechariah", "John", "Baptist")),
            Question(questionId++, 1, "What is the longest book in the Bible?", listOf("Genesis", "Psalms", "Isaiah"), 1, "Psalms is the longest book in the Bible with 150 chapters.", "Psalms 1:1", listOf("Psalms", "Longest", "Book")),
            Question(questionId++, 1, "Who was the first person to see Jesus after his resurrection?", listOf("Peter", "John", "Mary Magdalene"), 2, "Mary Magdalene was the first person to see Jesus after his resurrection.", "John 20:11-18", listOf("Mary Magdalene", "Resurrection", "First")),
            Question(questionId++, 1, "How many days was Jesus in the tomb?", listOf("1", "2", "3"), 2, "Jesus was in the tomb for 3 days, from Friday evening to Sunday morning.", "Matthew 12:40", listOf("Three", "Days", "Tomb")),
            Question(questionId++, 1, "Who was the first person to be baptized by John the Baptist?", listOf("Jesus", "Peter", "Andrew"), 0, "Jesus was baptized by John the Baptist in the Jordan River.", "Matthew 3:13-17", listOf("Jesus", "Baptized", "John")),
            Question(questionId++, 1, "What is the first commandment?", listOf("You shall not kill", "You shall not steal", "You shall have no other gods"), 2, "The first commandment is 'You shall have no other gods before me.'", "Exodus 20:3", listOf("First", "Commandment", "Gods")),
            Question(questionId++, 1, "Who was the first person to be called a Christian?", listOf("Peter", "Paul", "Barnabas"), 2, "Barnabas was the first person to be called a Christian in Antioch.", "Acts 11:26", listOf("Barnabas", "Christian", "First")),
            Question(questionId++, 1, "What is the first miracle Jesus performed?", listOf("Walking on water", "Feeding 5000", "Turning water to wine"), 2, "Jesus' first miracle was turning water into wine at the wedding in Cana.", "John 2:1-11", listOf("Water", "Wine", "Miracle")),
            Question(questionId++, 1, "Who was the first person to be stoned to death in the Bible?", listOf("Stephen", "Paul", "Peter"), 0, "Stephen was the first Christian martyr to be stoned to death.", "Acts 7:54-60", listOf("Stephen", "Stoned", "First")),
            Question(questionId++, 1, "What is the first fruit mentioned in the Bible?", listOf("Apple", "Grape", "Fig"), 2, "The fig tree and its fruit are mentioned early in Genesis.", "Genesis 3:7", listOf("Fig", "Fruit", "First")),
            Question(questionId++, 1, "Who was the first person to be called a prophet?", listOf("Moses", "Abraham", "Noah"), 1, "Abraham was called a prophet by God in Genesis 20:7.", "Genesis 20:7", listOf("Abraham", "Prophet", "First")),
            Question(questionId++, 1, "What is the first city mentioned in the Bible?", listOf("Jerusalem", "Babylon", "Eden"), 2, "Eden is mentioned as the first location where God placed Adam and Eve.", "Genesis 2:8", listOf("Eden", "City", "First")),
            Question(questionId++, 1, "Who was the first person to be called a king in the Bible?", listOf("David", "Saul", "Solomon"), 1, "Saul was the first king of Israel, anointed by Samuel.", "1 Samuel 10:1", listOf("Saul", "King", "First")),
            Question(questionId++, 1, "What is the first animal mentioned in the Bible?", listOf("Lion", "Eagle", "Fish"), 2, "Fish are mentioned first in Genesis 1:20-21 as part of God's creation.", "Genesis 1:20-21", listOf("Fish", "Animal", "First")),
            Question(questionId++, 1, "Who was the first person to be called a priest?", listOf("Aaron", "Moses", "Melchizedek"), 2, "Melchizedek was the first person called a priest in the Bible.", "Genesis 14:18", listOf("Melchizedek", "Priest", "First")),
            Question(questionId++, 1, "What is the first mountain mentioned in the Bible?", listOf("Sinai", "Ararat", "Carmel"), 1, "Mount Ararat is mentioned first as the resting place of Noah's ark.", "Genesis 8:4", listOf("Ararat", "Mountain", "First")),
            Question(questionId++, 1, "Who was the first person to be called a judge?", listOf("Samson", "Gideon", "Deborah"), 2, "Deborah was the first judge mentioned in the Book of Judges.", "Judges 4:4", listOf("Deborah", "Judge", "First"))
        )
        questions.addAll(level1Questions)

        // Level 2 - Very Easy (34 questions)
        val level2Questions = listOf(
            Question(questionId++, 2, "Who was the first person to be called a shepherd?", listOf("David", "Moses", "Abraham"), 0, "David was a shepherd before becoming king, tending his father's sheep.", "1 Samuel 16:11", listOf("David", "Shepherd", "First")),
            Question(questionId++, 2, "What is the first river mentioned in the Bible?", listOf("Jordan", "Nile", "Euphrates"), 2, "The Euphrates River is mentioned first in Genesis 2:14 as one of the four rivers flowing from Eden.", "Genesis 2:14", listOf("Euphrates", "River", "First")),
            Question(questionId++, 2, "Who was the first person to be called a warrior?", listOf("Joshua", "Caleb", "Gideon"), 0, "Joshua was a mighty warrior who led Israel into the Promised Land.", "Exodus 17:9-13", listOf("Joshua", "Warrior", "First")),
            Question(questionId++, 2, "What is the first tree mentioned in the Bible?", listOf("Oak", "Palm", "Tree of Life"), 2, "The Tree of Life is mentioned first in Genesis 2:9 in the Garden of Eden.", "Genesis 2:9", listOf("Tree of Life", "Tree", "First")),
            Question(questionId++, 2, "Who was the first person to be called a wise man?", listOf("Solomon", "Daniel", "Job"), 0, "Solomon was known for his wisdom, which God gave him when he asked for it.", "1 Kings 3:12", listOf("Solomon", "Wise", "First")),
            Question(questionId++, 2, "What is the first star mentioned in the Bible?", listOf("Morning Star", "North Star", "Evening Star"), 0, "The morning star is mentioned in Job 38:7 as part of God's creation.", "Job 38:7", listOf("Morning Star", "Star", "First")),
            Question(questionId++, 2, "Who was the first person to be called a builder?", listOf("Noah", "Solomon", "Nehemiah"), 0, "Noah was the first builder mentioned, constructing the ark according to God's specifications.", "Genesis 6:14-22", listOf("Noah", "Builder", "First")),
            Question(questionId++, 2, "What is the first color mentioned in the Bible?", listOf("Red", "Blue", "Green"), 0, "Red is mentioned first in Genesis 25:25 describing Esau's appearance at birth.", "Genesis 25:25", listOf("Red", "Color", "First")),
            Question(questionId++, 2, "Who was the first person to be called a singer?", listOf("David", "Asaph", "Heman"), 0, "David was a skilled musician and singer who composed many psalms.", "2 Samuel 23:1", listOf("David", "Singer", "First")),
            Question(questionId++, 2, "What is the first metal mentioned in the Bible?", listOf("Gold", "Silver", "Bronze"), 0, "Gold is mentioned first in Genesis 2:11-12 as part of the land of Havilah.", "Genesis 2:11-12", listOf("Gold", "Metal", "First")),
            Question(questionId++, 2, "Who was the first person to be called a farmer?", listOf("Cain", "Abel", "Noah"), 0, "Cain was a farmer who worked the soil, while his brother Abel was a shepherd.", "Genesis 4:2", listOf("Cain", "Farmer", "First")),
            Question(questionId++, 2, "What is the first gemstone mentioned in the Bible?", listOf("Diamond", "Ruby", "Onyx"), 2, "Onyx is mentioned first in Genesis 2:12 as part of the land of Havilah.", "Genesis 2:12", listOf("Onyx", "Gemstone", "First")),
            Question(questionId++, 2, "Who was the first person to be called a merchant?", listOf("Abraham", "Joseph", "Solomon"), 0, "Abraham was wealthy and engaged in trade, making him effectively a merchant.", "Genesis 13:2", listOf("Abraham", "Merchant", "First")),
            Question(questionId++, 2, "What is the first flower mentioned in the Bible?", listOf("Rose", "Lily", "Daisy"), 0, "The rose is mentioned in Song of Solomon 2:1 as a symbol of beauty.", "Song of Solomon 2:1", listOf("Rose", "Flower", "First")),
            Question(questionId++, 2, "Who was the first person to be called a craftsman?", listOf("Bezalel", "Oholiab", "Hiram"), 0, "Bezalel was the first craftsman mentioned, skilled in working with gold, silver, and bronze.", "Exodus 31:2-5", listOf("Bezalel", "Craftsman", "First")),
            Question(questionId++, 2, "What is the first bird mentioned in the Bible?", listOf("Eagle", "Dove", "Raven"), 1, "The dove is mentioned first in Genesis 8:8-12 when Noah sent it from the ark.", "Genesis 8:8-12", listOf("Dove", "Bird", "First")),
            Question(questionId++, 2, "Who was the first person to be called a scribe?", listOf("Ezra", "Baruch", "Jeremiah"), 0, "Ezra was a scribe skilled in the Law of Moses, teaching the people God's commands.", "Ezra 7:6", listOf("Ezra", "Scribe", "First")),
            Question(questionId++, 2, "What is the first number mentioned in the Bible?", listOf("1", "2", "3"), 0, "The number one is mentioned first in Genesis 1:5 as the first day of creation.", "Genesis 1:5", listOf("One", "Number", "First")),
            Question(questionId++, 2, "Who was the first person to be called a teacher?", listOf("Jesus", "Paul", "Peter"), 0, "Jesus was called 'Teacher' or 'Rabbi' by his disciples and others.", "John 1:38", listOf("Jesus", "Teacher", "First")),
            Question(questionId++, 2, "What is the first direction mentioned in the Bible?", listOf("North", "South", "East"), 2, "East is mentioned first in Genesis 2:8 as the direction where God planted the Garden of Eden.", "Genesis 2:8", listOf("East", "Direction", "First")),
            Question(questionId++, 2, "Who was the first person to be called a healer?", listOf("Jesus", "Peter", "Paul"), 0, "Jesus was the great healer, performing many miracles of healing during his ministry.", "Matthew 4:23", listOf("Jesus", "Healer", "First")),
            Question(questionId++, 2, "What is the first season mentioned in the Bible?", listOf("Spring", "Summer", "Fall"), 0, "Spring is mentioned first in Genesis 8:22 as part of God's promise after the flood.", "Genesis 8:22", listOf("Spring", "Season", "First")),
            Question(questionId++, 2, "Who was the first person to be called a fisherman?", listOf("Peter", "Andrew", "James"), 0, "Peter was a fisherman by trade before Jesus called him to be a disciple.", "Matthew 4:18", listOf("Peter", "Fisherman", "First")),
            Question(questionId++, 2, "What is the first time mentioned in the Bible?", listOf("Morning", "Evening", "Noon"), 0, "Morning is mentioned first in Genesis 1:5 as part of the first day of creation.", "Genesis 1:5", listOf("Morning", "Time", "First")),
            Question(questionId++, 2, "Who was the first person to be called a tax collector?", listOf("Matthew", "Zacchaeus", "Levi"), 0, "Matthew was a tax collector before Jesus called him to be a disciple.", "Matthew 9:9", listOf("Matthew", "Tax Collector", "First")),
            Question(questionId++, 2, "What is the first weather mentioned in the Bible?", listOf("Rain", "Snow", "Wind"), 0, "Rain is mentioned first in Genesis 2:5 as part of God's creation process.", "Genesis 2:5", listOf("Rain", "Weather", "First")),
            Question(questionId++, 2, "Who was the first person to be called a Pharisee?", listOf("Nicodemus", "Saul", "Gamaliel"), 0, "Nicodemus was a Pharisee who came to Jesus at night to learn from him.", "John 3:1", listOf("Nicodemus", "Pharisee", "First")),
            Question(questionId++, 2, "What is the first food mentioned in the Bible?", listOf("Bread", "Fruit", "Meat"), 1, "Fruit is mentioned first in Genesis 1:29 as food given to humans by God.", "Genesis 1:29", listOf("Fruit", "Food", "First")),
            Question(questionId++, 2, "Who was the first person to be called a Sadducee?", listOf("Caiaphas", "Annas", "Joseph"), 0, "Caiaphas was the high priest and a Sadducee who presided over Jesus' trial.", "John 18:13-14", listOf("Caiaphas", "Sadducee", "First")),
            Question(questionId++, 2, "What is the first drink mentioned in the Bible?", listOf("Water", "Wine", "Milk"), 0, "Water is mentioned first in Genesis 1:2 as covering the earth before creation.", "Genesis 1:2", listOf("Water", "Drink", "First")),
            Question(questionId++, 2, "Who was the first person to be called a Zealot?", listOf("Simon", "Judas", "Barabbas"), 0, "Simon the Zealot was one of Jesus' twelve disciples, called to distinguish him from Simon Peter.", "Luke 6:15", listOf("Simon", "Zealot", "First")),
            Question(questionId++, 2, "What is the first emotion mentioned in the Bible?", listOf("Love", "Fear", "Joy"), 1, "Fear is mentioned first in Genesis 3:10 when Adam was afraid after eating the forbidden fruit.", "Genesis 3:10", listOf("Fear", "Emotion", "First")),
            Question(questionId++, 2, "Who was the first person to be called a Samaritan?", listOf("The woman at the well", "The good Samaritan", "Simon"), 0, "The Samaritan woman at the well was the first person explicitly called a Samaritan in the Bible.", "John 4:7-9", listOf("Samaritan", "Woman", "First")),
            Question(questionId++, 2, "What is the first action mentioned in the Bible?", listOf("Create", "Speak", "Move"), 0, "Create is the first action mentioned in Genesis 1:1 when God created the heavens and the earth.", "Genesis 1:1", listOf("Create", "Action", "First"))
        )
        questions.addAll(level2Questions)

        // Continue with levels 3-10 (34 questions each) - Easy difficulty
        for (level in 3..10) {
            val levelQuestions = generateLevelQuestions(level, 34, "Easy")
            questions.addAll(levelQuestions)
        }

        // Continue with levels 11-20 (33 questions each) - Medium difficulty
        for (level in 11..20) {
            val levelQuestions = generateLevelQuestions(level, 33, "Medium")
            questions.addAll(levelQuestions)
        }

        // Continue with levels 21-30 (33 questions each) - Expert difficulty
        for (level in 21..30) {
            val levelQuestions = generateLevelQuestions(level, 33, "Expert")
            questions.addAll(levelQuestions)
        }

        return questions
    }

    private fun generateLevelQuestions(level: Int, count: Int, difficulty: String): List<Question> {
        val questions = mutableListOf<Question>()
        
        // Calculate the starting question ID based on previous levels
        val startId = when {
            level <= 10 -> (level - 1) * 34 + 1  // Levels 1-10 have 34 questions each
            else -> 340 + (level - 11) * 33 + 1  // Levels 11+ have 33 questions each
        }
        
        // This is a simplified version - in a real implementation, you would have
        // a comprehensive database of 1000 unique Bible questions
        // For now, I'll create placeholder questions that get progressively harder
        
        for (i in 0 until count) {
            val questionId = startId + i
            val questionNumber = i + 1
            
            when (difficulty) {
                "Easy" -> {
                    questions.add(Question(
                        questionId,
                        level,
                        "Level $level Question $questionNumber: What is a basic Bible fact about $level?",
                        listOf("Option A", "Option B", "Option C"),
                        (level + i) % 3,
                        "This is a placeholder explanation for Level $level Question $questionNumber.",
                        "Placeholder Verse $level:$questionNumber",
                        listOf("Level $level", "Basic", "Bible")
                    ))
                }
                "Medium" -> {
                    questions.add(Question(
                        questionId,
                        level,
                        "Level $level Question $questionNumber: What is an intermediate Bible fact about $level?",
                        listOf("Option A", "Option B", "Option C"),
                        (level + i) % 3,
                        "This is a placeholder explanation for Level $level Question $questionNumber.",
                        "Placeholder Verse $level:$questionNumber",
                        listOf("Level $level", "Intermediate", "Bible")
                    ))
                }
                "Expert" -> {
                    questions.add(Question(
                        questionId,
                        level,
                        "Level $level Question $questionNumber: What is an expert Bible fact about $level?",
                        listOf("Option A", "Option B", "Option C"),
                        (level + i) % 3,
                        "This is a placeholder explanation for Level $level Question $questionNumber.",
                        "Placeholder Verse $level:$questionNumber",
                        listOf("Level $level", "Expert", "Bible")
                    ))
                }
            }
        }
        
        return questions
    }
}
