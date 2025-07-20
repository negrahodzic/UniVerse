# UniVerse 📚

A gamified study companion Android app that transforms individual and group study sessions into engaging, competitive experiences. UniVerse combines social learning with gamification elements to motivate students and enhance their academic performance.

## 🌟 Features

### 📖 Study Sessions
- **Create Study Sessions**: Set custom duration and earn points for completing sessions
- **Join Sessions**: Scan QR codes to join friends' study sessions
- **Real-time Tracking**: Monitor study progress with live session timers
- **Session History**: View detailed records of all completed study sessions

### 🏆 Gamification System
- **Points System**: Earn points for completing study sessions and attending events
- **Level Progression**: Advance through levels based on your study achievements
- **Achievements**: Unlock badges and achievements for various milestones
- **Streaks**: Track consecutive study days and maintain consistency

### 👥 Social Features
- **Friend System**: Add friends via NFC or QR code scanning
- **Leaderboards**: Compete with friends and global rankings
- **Study Groups**: Join group study sessions with real-time collaboration
- **Social Profiles**: View and compare study statistics with peers

### 🎯 Events & Activities
- **University Events**: Discover and attend academic events
- **Event Tickets**: Manage event registrations and attendance
- **Event History**: Track your participation in university activities

### 🏫 Multi-Organization Support
- **Organization Selection**: Choose your university/institution
- **Custom Branding**: Each organization has unique themes and branding
- **Domain Verification**: Secure email verification for organization members

### 📱 Technical Features
- **NFC Integration**: Quick friend adding and session joining via NFC
- **QR Code Scanning**: Easy session joining and friend connections
- **Real-time Updates**: Live synchronization with Firebase backend
- **Offline Support**: Core functionality works without internet connection

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21 or higher
- Google Play Services
- NFC-enabled device (for NFC features)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/negrahodzic/UniVerse.git
   cd UniVerse
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the project folder
   - Sync Gradle files

3. **Configure Firebase**
   - Create a Firebase project
   - Add your `google-services.json` file to the `app/` directory
   - Enable Authentication and Firestore in Firebase Console

4. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio

## 📱 App Structure

### Core Activities
- **MainActivity**: Organization selection and initial setup
- **DashboardActivity**: Main hub with stats and quick actions
- **StudySessionActivity**: Create and join study sessions
- **EventsActivity**: Browse and manage university events
- **LeaderboardActivity**: View rankings and achievements
- **ProfileActivity**: User profile and settings

### Key Components
- **Models**: User, StudySession, Event, Achievement data structures
- **Adapters**: RecyclerView adapters for lists and grids
- **Managers**: Business logic for users, organizations, and achievements
- **Repositories**: Data access layer for Firebase operations
- **Utils**: Helper classes for NFC, QR codes, and theming

## 🎨 Customization

### Organization Themes
Each organization can have custom:
- Color schemes
- Logos and branding
- Welcome messages
- Feature availability

### User Experience
- Dark/Light theme support
- Customizable study session durations
- Flexible point systems
- Configurable achievement criteria

## 🔧 Technical Stack

- **Language**: Java
- **Platform**: Android (API 21+)
- **Backend**: Firebase (Authentication, Firestore, Cloud Functions)
- **UI Framework**: Material Design Components
- **QR Scanning**: ZXing library
- **NFC**: Android NFC API
- **Image Loading**: Glide
- **Real-time Updates**: Firebase Realtime Database

## 📊 Data Models

### User
- Profile information (username, email, organization)
- Study statistics (points, level, streak days)
- Social connections (friends, achievements)
- Session history and event attendance

### StudySession
- Session metadata (duration, points, participants)
- Real-time status tracking
- QR code generation for joining
- Completion statistics

### Event
- Event details (title, description, location, date)
- Ticket management
- Attendance tracking
- Organization-specific branding

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Firebase for backend services
- Material Design for UI components
- ZXing for QR code functionality
- Android NFC API for contactless features

## 📞 Support

For support, email support@universe-app.com or create an issue in this repository.

---

**UniVerse** - Transforming study sessions into engaging social experiences! 🚀 