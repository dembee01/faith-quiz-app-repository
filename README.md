# Faith Quiz App

A comprehensive Bible quiz application built with Android Jetpack Compose, featuring 30 levels of progressively challenging questions.

## Features

- **30 Progressive Levels**: From basic to expert Bible knowledge
- **Interactive Quiz Interface**: Modern Material Design 3 UI
- **Progress Tracking**: Save your progress and track your performance
- **Leaderboard**: Compare scores with other players
- **Daily Challenges**: New questions every day
- **Settings & Customization**: Personalize your quiz experience

## Technical Stack

- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room Database with SQLite
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose
- **Data Persistence**: DataStore Preferences
- **Coroutines**: For asynchronous operations

## Recent Stability Improvements

### Fixed Issues:
1. **Database Migration**: Added fallback migration strategy to prevent crashes
2. **Error Handling**: Comprehensive try-catch blocks throughout the app
3. **Navigation Safety**: Added error handling for navigation operations
4. **Global Error Handler**: Prevents uncaught exceptions from crashing the app
5. **Improved Logging**: Better error tracking and debugging information

### Performance Optimizations:
- Efficient database operations with proper error handling
- Optimized UI rendering with Compose
- Memory-efficient question loading
- Background database initialization

## Installation

1. Clone the repository
2. Open in Android Studio
3. Build and run on your device or emulator

## Troubleshooting

### App Crashes on Startup
If the app crashes immediately after opening:

1. **Clear App Data**: Go to Settings > Apps > Faith Quiz > Storage > Clear Data
2. **Restart Device**: Sometimes a simple restart helps
3. **Check Logs**: Use `adb logcat` to view detailed error logs
4. **Reinstall**: Uninstall and reinstall the app

### Database Issues
If you experience database-related problems:

1. The app now has automatic database recovery
2. Database will be recreated if corrupted
3. All progress will be reset if database is recreated

### Performance Issues
- Ensure you have at least 2GB of free RAM
- Close other apps while using Faith Quiz
- Restart the app if it becomes slow

## Development

### Building the Project
```bash
./gradlew assembleDebug
```

### Running Tests
```bash
./gradlew test
```

### Code Quality
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add proper error handling
- Include unit tests for critical functionality

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review the error logs
3. Create an issue with detailed information about the problem

---

**Note**: This app is designed for educational purposes and Bible study. All questions are based on biblical content and are meant to enhance understanding of scripture.
