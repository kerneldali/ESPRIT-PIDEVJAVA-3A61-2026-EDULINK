# EduLink Java — Major Feature Implementation Plan

This plan covers four major feature groups to be implemented in the existing JavaFX project:
1. **AI-Powered Assistance & Help Request Module**
2. **Community Feed Moderation**
3. **User Auth Improvements** (Face ID, Password Reset Email, CAPTCHA)
4. **Web3 Credit System** (ERC-20 Token on Ethereum Sepolia Testnet)

---

## User Review Required

> [!IMPORTANT]
> **Web3 Smart Contract**: You mentioned you will handle Sepolia deployment and verification yourself. I will write the full Solidity contract and the Java integration layer. You will need to:
> 1. Deploy the `EduToken.sol` contract on Sepolia
> 2. Paste the deployed contract address into `Web3Config.java`
> 3. The Java app will interact with it via Web3j

> [!WARNING]
> **Face ID**: JavaFX doesn't have native camera access through FXML. The Face ID feature will be implemented using **OpenCV + webcam** via a separate JNI/process call. You will need OpenCV for Java installed. If you prefer a simplified simulation-mode that stores a face hash without real camera capture, I can implement that instead. **Please confirm** before I start:
> - **Option A**: Real OpenCV webcam → face encoding → compare on login (requires OpenCV dependency)
> - **Option B**: Simulated — stores a "face hash" (SHA-256 of a secret phrase) as a placeholder (no extra dependency)

> [!IMPORTANT]
> **Groq API Key**: The AI features (classification, smart matching, toxicity, suggestions, summarization) will all use the existing `GroqService` with the Groq API (which uses LLaMA). You must replace `dummy_key_to_be_replaced` in `GroqConfig.java` with your real Groq API key.

> [!NOTE]
> **CAPTCHA**: Since this is a desktop app (no browser), we will implement a **custom image-based CAPTCHA** (shows a distorted math problem as a JavaFX Canvas image). This is functionally equivalent and doesn't require Google reCAPTCHA.

---

## Proposed Changes

### Part 1 — Database Schema Extensions

#### [NEW] SQL Migration Script
Run in MySQL to add new tables and columns:
- `help_session` table (tutor, student, jitsi_room, summary, is_active)
- `chat_message` table (session_id, sender_id, content, is_toxic, sentiment, language, timestamp)
- `post_reaction` table (post_id, user_id, type)
- `post_report` table (post_id, reporter_id, reason, status)
- Add `eth_wallet_address` column to `user` table
- Add `otp_code` + `otp_expires_at` + `face_descriptor` columns (already partially there)
- `token_transaction` table (from_user, to_user, amount, tx_hash, type)

---

### Part 2 — AI Services Layer

#### [NEW] `AiClassificationService.java`
Calls GroqService to classify a help request by category (Math, Science, Code, Language, etc.) and difficulty (EASY, MEDIUM, HARD) based on title + description.

#### [NEW] `SmartMatchingService.java`
Fetches all users with ROLE_FACULTY, analyzes their past resolved sessions, and uses AI to rank best tutors for a given help request.

#### [NEW] `ToxicityService.java`
Wraps GroqService to analyze text for toxicity/hate speech. Returns `{isToxic: boolean, reason: string}`. Used in both chat and community posts.

#### [NEW] `SentimentService.java`
Detects language and emotional sentiment of a message. Non-blocking, used for analytics.

#### [NEW] `SessionSummaryService.java`
Takes a list of chat messages and sends them to Groq to generate a readable summary of the tutoring session.

#### [NEW] `SuggestedRepliesService.java`
Takes recent chat history and returns 3 AI-suggested reply snippets.

---

### Part 3 — Assistance / Help Request Module

#### [MODIFY] `HelpRequest.java`
- Add `tutorId`, `jitsiRoomId`, `sessionSummary` fields.

#### [NEW] `HelpSession.java` (model)
Represents a tutoring session: `id`, `helpRequestId`, `tutorId`, `studentId`, `jitsiRoomId`, `summary`, `isActive`, `startedAt`, `endedAt`.

#### [NEW] `ChatMessage.java` (model)
Represents a chat message: `id`, `sessionId`, `senderId`, `content`, `isToxic`, `sentiment`, `language`, `timestamp`.

#### [NEW] `HelpSessionService.java`
Full CRUD for sessions and messages, including:
- `createSession(helpRequestId, tutorId)` — **Bounty Escrow**: Locks the student's tokens in the DB/Contract to guarantee payment.
- `closeSession(sessionId)` — **Anti-Farming & Transfer**: 
    - Triggers `AiSessionSummary` to evaluate session quality.
    - If quality score > 70% and engagement metrics are met, transfers locked bounty to tutor's wallet via `EduTokenService`.
    - Generates AI summary and sends email.
- `sendMessage(sessionId, senderId, content)` — runs toxicity check before saving
- `getMessages(sessionId)` — returns all messages for display

#### [MODIFY] `HelpRequestListController.java`
- Add "Join as Tutor" button to each open card (for non-owner non-admin users)
- Add "View Session" button for in-progress requests

#### [NEW] `ChatController.java`
Full chat UI:
- Scrollable message list with bubbles (mine vs others)
- AI-suggested reply buttons at the bottom
- Toxicity warning badge on flagged messages
- "Open Jitsi Video" button (opens browser to `meet.jit.si/{roomId}`)
- "Close Session" button triggers AI summarization + bounty transfer + email

#### [MODIFY] `HelpRequestFormController.java`
- On form submission: calls `AiClassificationService` and auto-fills category/difficulty labels
- Validates bounty ≤ user's wallet balance

#### [MODIFY] `AdminAssistanceController.java`
- Add Analytics tab: bar chart for monthly trends (using JavaFX Charts API — no extra dependency)
- Top tutors ranking table
- Session resolution rate
- Toxic message list with export

#### [NEW] FXML: `Chat.fxml`
New view for the in-session chat interface.

---

### Part 4 — Community Feed Module

#### [MODIFY] `CommunityBoardController.java`
- **Before posting**: Run `ToxicityService.analyze(content)` → if toxic, reject with warning
- **Reactions**: Replace old upvote with `PostReactionBar` (Like 👍, Love ❤️, Insightful 💡, Funny 😂, Support 🤝)
- **Report**: Improve `actionReportThread` to save to `post_report` table with reporter info
- **Admin panel**: New `AdminCommunityPostController` embedded in admin section

#### [MODIFY] `ForumService.java`
- Add `addReaction(postId, userId, type)`, `getReactions(postId)`, `addReport(postId, reporterId, reason)`, `getPendingReports()` methods

#### [NEW] `AdminCommunityController.java`
- Lists all pending reports
- Dismisses or Actions (deletes post + cascades) reports

#### [NEW] FXML: `AdminCommunity.fxml`

---

### Part 5 — User Auth Improvements

#### [MODIFY] `LoginController.java`
- **Forgot Password link**: Opens a dialog to enter email, generates a 6-digit OTP, sends via `MailService.sendOtpEmail()`, shows second dialog to enter OTP + new password.
- **CAPTCHA on Register**: Displays a custom math CAPTCHA canvas (e.g., "5 + 3 = ?") before allowing registration submission.

#### [MODIFY] `MailService.java`
- Add `sendOtpEmail(String toEmail, String otp)` method

#### [NEW] `CaptchaService.java`
- Generates a math question and renders it as a JavaFX `Canvas` with a distorted font
- `validate(userAnswer)`: returns true/false

#### [MODIFY `UserService.java`]
- Add `saveOtp(userId, otpCode)`, `findByOtp(otp)`, `resetPassword(userId, newPassword)`, `saveFaceDescriptor(userId, descriptor)`, `findByFaceDescriptor(descriptor)` methods

#### [NEW] `FaceIdService.java`
- **If Option A (OpenCV)**: Captures from webcam, encodes face as 128D-vector, stores Base64 in DB, matches on login
- **If Option B (Simulated)**: Stores SHA-256 of chosen phrase, matches on login with same phrase

#### [MODIFY] `UserProfileController.java`
- Add "Setup Face ID" button that opens the FaceId flow
- Shows whether Face ID is active for the current user

#### [MODIFY] `Login.fxml`
- Add "Login with Face ID" button
- Add "Forgot Password" link
- Add CAPTCHA canvas + answer field to the register section

---

### Part 6 — Web3 Credit System (Ethereum Sepolia)

> [!IMPORTANT]
> This is the most architecturally complex feature. The strategy is:
> - **Solidity**: A standard ERC-20-compatible token (`EduToken`) with a custom `buyTokens()` payable function and admin `mint()`/`distributeBounty()` functions
> - **Java**: Uses **Web3j** library to call the contract. All signing happens client-side with per-user private keys stored encrypted in DB.

#### [NEW] `EduToken.sol` (Solidity Smart Contract)
Key features:
- Standard ERC-20 (name: "EduLink Token", symbol: "EDU")
- `buyTokens()` payable: Users send ETH → get EDU tokens at a fixed rate
- `transfer()` & `transferFrom()`: Standard token transfers
- `distributeBounty(address tutor, uint256 amount)`: Admin-only function to transfer bounty from student to tutor (called by backend when session closes)
- `getBalance(address user)`: Returns token balance
- Event emissions for all transfers

#### [MODIFY] `pom.xml`
Add Web3j dependency:
```xml
<dependency>
    <groupId>org.web3j</groupId>
    <artifactId>core</artifactId>
    <version>4.10.3</version>
</dependency>
```

#### [NEW] `Web3Config.java`
- Stores Sepolia RPC URL, deployed contract address, admin private key (for server-side txns)

#### [NEW] `EduTokenService.java`
- Connects to Sepolia via Web3j
- `getBalance(ethAddress)` → EDU token balance
- `buyTokens(ethAddress, privateKey, ethAmount)` → calls payable `buyTokens()`
- `transferTokens(fromAddress, fromKey, toAddress, amount)` → ERC-20 transfer
- `distributeBounty(studentAddress, tutorAddress, amount, sessionId)` → admin-signed call. **Includes anti-farming validation**:
    - **Session Quality Check**: Calls AI to verify if the session was productive before allowing the on-chain transfer.
    - **Engagement Metrics**: Verifies minimum message count (e.g., > 5) and duration (e.g., > 5 mins).
    - **Rate Limiting**: Detects and flags suspicious repeat sessions between the same two users.
- `syncBalanceToDb(userId, balance)` → mirrors on-chain balance to `wallet_balance` column

#### [NEW] `UserWalletController.java`
Dedicated wallet view:
- Current EDU token balance (on-chain via `getBalance()`)
- ETH wallet address registration/display
- "Buy EDU Tokens" → input ETH amount → calls `buyTokens()` → shows tx hash + Etherscan link
- "Send Tokens" → input recipient email + amount → calls `transferTokens()`
- Transaction history table (from `token_transaction` DB table)
- Exchange rate widget (e.g., 1 ETH = 1000 EDU)

#### [MODIFY] `AdminWalletManagementController.java`
- Add "Mint Tokens" admin function → calls contract `mint(address, amount)` 
- Show total supply on-chain

#### [NEW] FXML: `Web3Wallet.fxml`

---

## Verification Plan

### Automated
- Verify all new Java files compile without errors
- Verify new DB tables exist after running the migration SQL

### Manual Verification
1. **Auth**: Register with CAPTCHA, test forgot password OTP flow via email
2. **Help Request**: Create request → verify AI category/difficulty auto-fill → Join as tutor → open chat → send messages → verify toxicity filter flags bad content → close session → verify email with AI summary
3. **Community**: Post with clean content → react with multiple emoji → report a post as admin → dismiss/action report
4. **Web3 Wallet**: Connect ETH wallet → buy tokens → verify balance appears on Sepolia Etherscan → post a bounty help request → resolve it → verify tokens transferred on-chain

---

## Open Questions

> [!IMPORTANT]
> 1. **Face ID**: Option A (Real OpenCV) or Option B (Simulated)?
> 2. **Groq API Key**: Do you have a real Groq key to put in `GroqConfig.java`? The AI features won't work until this is set.
> 3. **Admin ETH Key**: For the Web3 `distributeBounty()` call, an admin private key needs to be hardcoded in `Web3Config.java`. This should be a **Sepolia testnet** key only (no real ETH). Do you have one to provide, or should I generate a placeholder?
> 4. **Gmail Credentials**: The existing `MailService.java` uses `zariatyassine1@gmail.com`. Should all new emails (OTP, session summary) use the same account?
