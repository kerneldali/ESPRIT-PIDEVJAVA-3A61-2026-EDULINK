package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Enrollment;
import com.edulink.gui.models.courses.Resource;

import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.services.courses.ResourceService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;

public class CourseDetailsController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private Label courseTitleLabel;
    @FXML private Label courseDescLabel;
    @FXML private Label xpLabel;
    @FXML private Label levelLabel;
    @FXML private VBox notEnrolledBox;
    @FXML private VBox enrolledBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label enrollmentStatusLabel;
    @FXML private Button certifyBtn;
    @FXML private VBox resourcesContainer;

    // Suggest overlay
    @FXML private VBox formOverlay;
    @FXML private Label suggestContextLabel;
    @FXML private TextField suggestTitleField;
    @FXML private ComboBox<String> suggestTypeCombo;
    @FXML private TextField suggestUrlField;
    @FXML private Button browseSuggestBtn;
    @FXML private Button suggestSaveBtn;
    @FXML private Label suggestTitleError;

    private Course currentCourse;
    private Enrollment currentEnrollment;
    private ResourceService resourceService = new ResourceService();
    private EnrollmentService enrollmentService = new EnrollmentService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        suggestTypeCombo.setItems(FXCollections.observableArrayList("PDF"));
        suggestTypeCombo.setValue("PDF");
        suggestTypeCombo.setDisable(true);

        suggestTitleField.textProperty().addListener((obs, o, n) -> {
            suggestTitleError.setText("");
            suggestSaveBtn.setDisable(n == null || n.trim().isEmpty());
            if (n != null && !n.trim().isEmpty() && n.trim().length() < 2) {
                suggestTitleError.setText("At least 2 characters");
                suggestSaveBtn.setDisable(true);
            }
        });

        browseSuggestBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Documents", "*.pdf"));
            java.io.File file = fc.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) suggestUrlField.setText(file.getAbsolutePath());
        });
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        courseTitleLabel.setText(course.getTitle());
        courseDescLabel.setText(course.getDescription() != null ? course.getDescription() : "");
        xpLabel.setText("+" + course.getXp() + " XP on completion");
        levelLabel.setText("Level: " + (course.getLevel() != null ? course.getLevel() : "N/A"));

        checkEnrollment();
        loadResources();
    }

    private void checkEnrollment() {
        int studentId = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
        List<Enrollment> enrollments = enrollmentService.getEnrollmentByStudent(studentId);
        currentEnrollment = enrollments.stream()
                .filter(e -> e.getCoursId() == currentCourse.getId())
                .findFirst().orElse(null);

        if (currentEnrollment != null) {
            notEnrolledBox.setVisible(false);
            notEnrolledBox.setManaged(false);
            enrolledBox.setVisible(true);
            enrolledBox.setManaged(true);
            
            // Re-calculate progress from resources directly to be safe
            int total = resourceService.findByCourse(currentCourse.getId()).size();
            int completed = resourceService.getCompletedCount(studentId, currentCourse.getId());
            double progress = (total > 0) ? (double) completed / total * 100.0 : 0.0;
            
            // Sync with DB if different
            if (Math.abs(progress - currentEnrollment.getProgress()) > 0.01) {
                currentEnrollment.setProgress(progress);
                enrollmentService.edit(currentEnrollment);
            }

            progressBar.setProgress(progress / 100.0);
            progressLabel.setText(Math.round(progress) + "% Completed");
            
            if (progress >= 100) {
                enrollmentStatusLabel.setText("✅ COMPLETED — Awarded " + currentCourse.getXp() + " XP!");
                enrollmentStatusLabel.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");
                certifyBtn.setVisible(true);
                certifyBtn.setManaged(true);
            } else {
                enrollmentStatusLabel.setText("IN PROGRESS — " + Math.round(progress) + "%");
                enrollmentStatusLabel.setStyle("-fx-text-fill: #a0a0ab;");
                certifyBtn.setVisible(false);
                certifyBtn.setManaged(false);
            }
        } else {
            notEnrolledBox.setVisible(true);
            notEnrolledBox.setManaged(true);
            enrolledBox.setVisible(false);
            enrolledBox.setManaged(false);
        }
    }

    private void loadResources() {
        resourcesContainer.getChildren().clear();
        List<Resource> resources = resourceService.findByCourse(currentCourse.getId())
                .stream().filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus())).toList();

        if (resources.isEmpty()) {
            Label empty = new Label("No resources available yet.");
            empty.setStyle("-fx-text-fill: #a0a0ab;");
            resourcesContainer.getChildren().add(empty);
            return;
        }

        for (Resource r : resources) {
            HBox row = new HBox(15);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #ffffff11;");

            // Icon
            Label icon = new Label("📄");
            icon.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 16px;");

            VBox infoBox = new VBox(3);
            Label title = new Label(r.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14;");
            Label type = new Label(r.getType());
            type.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11px; -fx-font-weight: bold;");
            infoBox.getChildren().addAll(title, type);
            infoBox.setPrefWidth(200);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button summaryBtn = new Button("📄 Summary");
            summaryBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            summaryBtn.setOnAction(e -> handleGenerateSummary(r));

            Button quizBtn = new Button("❓ Quiz");
            quizBtn.setStyle("-fx-background-color: #ec4899; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            quizBtn.setOnAction(e -> handleGenerateQuiz(r));

            Button viewBtn = new Button("▶ Open");
            viewBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            viewBtn.setOnAction(e -> openResource(r));

            Button completeBtn = new Button("✔ Complete");
            completeBtn.setDisable(currentEnrollment == null);

            int studentId = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
            boolean isDone = resourceService.isResourceCompleted(studentId, r.getId());

            if (isDone) {
                completeBtn.setText("✔ Done");
                completeBtn.setStyle("-fx-background-color: #3b82f633; -fx-text-fill: #3b82f6; -fx-background-radius: 5;");
                completeBtn.setDisable(true);
            } else {
                completeBtn.setStyle("-fx-background-color: #00d289; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
            }

            completeBtn.setOnAction(e -> {
                if (currentEnrollment != null) {
                    resourceService.markAsCompleted(studentId, r.getId());
                    checkEnrollment(); // This will auto-update progress in DB too
                    loadResources();
                    if (currentEnrollment.getProgress() >= 100) {
                        // Reward student
                        int sid = com.edulink.gui.util.SessionManager.getCurrentUser().getId();
                        int xpReward = currentCourse.getXp();
                        com.edulink.gui.services.UserService us = new com.edulink.gui.services.UserService();
                        us.updateXp(sid, xpReward);
                        us.updateWallet(sid, (double)xpReward); // Match points to wallet balance as requested
                        
                        EduAlert.show(EduAlert.AlertType.SUCCESS, "Course Completed!", 
                            "Congratulations! You gained " + xpReward + " XP and the same amount of coins in your wallet.");
                        
                        checkEnrollment(); // Refresh UI to show Get Certified button
                    }
                }
            });

            row.getChildren().addAll(icon, infoBox, spacer, summaryBtn, quizBtn, viewBtn, completeBtn);
            resourcesContainer.getChildren().add(row);
        }
    }

    private void openResource(Resource r) {
        try {
            String path = r.getUrl();
            if (path == null || path.trim().isEmpty()) {
                EduAlert.show(EduAlert.AlertType.WARNING, "No Path", "No URL/path set for this resource.");
                return;
            }
            
            // Resolve relative path if needed
            File file = new File(path);
            if (!file.exists() && !file.isAbsolute() && !path.startsWith("http")) {
                File relFile = new File(System.getProperty("user.dir"), path);
                if (relFile.exists()) {
                    file = relFile;
                }
            }

            try {
                if (file.exists()) {
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()).start();
                    } else if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } else if (path.startsWith("http")) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(path));
                    }
                } else {
                    EduAlert.show(EduAlert.AlertType.ERROR, "File Not Found", "Could not locate the PDF file at:\n" + path);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                EduAlert.show(EduAlert.AlertType.ERROR, "Viewer Error", ex.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            EduAlert.show(EduAlert.AlertType.ERROR, "Viewer Error", ex.getMessage());
        }
    }

    @FXML
    private void handleGenerateCertificate() {
        if (currentEnrollment == null || currentEnrollment.getProgress() < 100) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Not Ready", "Complete all resources first!");
            return;
        }

        com.edulink.gui.models.User student = com.edulink.gui.util.SessionManager.getCurrentUser();
        com.edulink.gui.services.PdfExportService exporter = new com.edulink.gui.services.PdfExportService();

        // Format Selection Dialog
        ButtonType pdfBtn = new ButtonType("🎓 Professional PDF", ButtonBar.ButtonData.OK_DONE);
        ButtonType pngBtn = new ButtonType("🖼️ High-Res Image (PNG)", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Elite Certification");
        alert.setHeaderText("Congratulations, " + student.getFullName() + "!");
        alert.setContentText("Your achievement is verified. How would you like to save your certificate?");
        alert.getButtonTypes().setAll(pdfBtn, pngBtn, cancelBtn);
        alert.initOwner(rootPane.getScene().getWindow());

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != cancelBtn) {
            boolean isPdf = result.get() == pdfBtn;
            
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Certificate");
            fc.setInitialFileName("Certificate_" + currentCourse.getTitle().replace(" ", "_") + (isPdf ? ".pdf" : ".png"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(isPdf ? "PDF Document" : "PNG Image", isPdf ? "*.pdf" : "*.png"));

            File file = fc.showSaveDialog(rootPane.getScene().getWindow());
            if (file != null) {
                try {
                    if (isPdf) {
                        exporter.exportCertificate(student, currentCourse, file);
                    } else {
                        exporter.exportCertificateAsImage(student, currentCourse, file);
                    }
                    
                    EduAlert.show(EduAlert.AlertType.SUCCESS, "Export Success", 
                        "Your certificate has been saved to:\n" + file.getAbsolutePath());
                    
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()).start();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    EduAlert.show(EduAlert.AlertType.ERROR, "Export Error", ex.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleEnroll() {
        int sid = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
        System.out.println("🚀 [DEBUG] User clicking Enroll. StudentID=" + sid + ", CourseID=" + currentCourse.getId());

        Enrollment e = new Enrollment();
        e.setStudentId(sid);
        e.setCoursId(currentCourse.getId());
        e.setProgress(0.0);
        e.setEnrolledAt(LocalDateTime.now());
        e.setLastAccessed(LocalDateTime.now());

        enrollmentService.add2(e);
        EduAlert.show(EduAlert.AlertType.SUCCESS, "Success!", "You are now enrolled in \"" + currentCourse.getTitle() + "\".");
        
        checkEnrollment();
        loadResources();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/CourseList.fxml"));
            Parent root = loader.load();
            CourseListController controller = loader.getController();
            com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
            com.edulink.gui.models.courses.Matiere m = ms.getAll().stream()
                    .filter(x -> x.getId() == currentCourse.getMatiereId())
                    .findFirst().orElse(null);
            if (m != null) controller.setMatiere(m);
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSuggestResource() {
        suggestTitleField.clear();
        suggestUrlField.clear();
        suggestTypeCombo.setValue("PDF");
        suggestTitleError.setText("");
        suggestSaveBtn.setDisable(true);
        suggestContextLabel.setText("For course: " + (currentCourse != null ? currentCourse.getTitle() : ""));
        formOverlay.setVisible(true);
        formOverlay.toFront();
    }

    @FXML
    private void handleCloseSuggest() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleSubmitSuggest() {
        Resource p = new Resource();
        p.setCoursId(currentCourse.getId());
        p.setTitle(suggestTitleField.getText().trim());
        p.setType("PDF");
        
        String savedPath = saveFileToProject(suggestUrlField.getText() != null ? suggestUrlField.getText().trim() : "");
        p.setUrl(savedPath);
        
        boolean isAdmin = false;
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            com.edulink.gui.models.User u = com.edulink.gui.util.SessionManager.getCurrentUser();
            p.setAuthorId(u.getId());
            if (u.hasRole("ROLE_ADMIN") || u.hasRole("ROLE_FACULTY")) {
                isAdmin = true;
            }
        } else {
            p.setAuthorId(1);
        }
        
        p.setStatus(isAdmin ? "ACCEPTED" : "PENDING");

        resourceService.add2(p);
        
        // Refresh immediately if it's automatically accepted
        loadResources();

        handleCloseSuggest();
        
        if (isAdmin) {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Resource Added",
                    "The resource has been automatically accepted and added.");
        } else {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Proposal Submitted",
                    "Your resource suggestion has been sent to admin for review.");
        }
    }

    private String saveFileToProject(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) return sourcePath;
        java.io.File sourceFile = new java.io.File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isAbsolute()) return sourcePath;
        
        java.io.File destDir = new java.io.File(System.getProperty("user.dir"), "src/main/resources/pdfs");
        if (!destDir.exists() && new java.io.File(System.getProperty("user.dir"), "java/src/main/resources").exists()) {
            destDir = new java.io.File(System.getProperty("user.dir"), "java/src/main/resources/pdfs");
        } else if (!destDir.exists()) {
            destDir = new java.io.File("src/main/resources/pdfs");
        }
        
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        java.io.File destFile = new java.io.File(destDir, sourceFile.getName());
        int counter = 1;
        while (destFile.exists() && !destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
            String name = sourceFile.getName();
            int dotIndex = name.lastIndexOf(".");
            String base = dotIndex > 0 ? name.substring(0, dotIndex) : name;
            String ext = dotIndex > 0 ? name.substring(dotIndex) : "";
            destFile = new java.io.File(destDir, base + "_" + counter + ext);
            counter++;
        }
        
        if (!destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
            try {
                java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return "src/main/resources/pdfs/" + destFile.getName();
            } catch (Exception e) {
                System.err.println("Failed to copy file: " + e.getMessage());
                return sourcePath;
            }
        }
        return "src/main/resources/pdfs/" + destFile.getName();
    }

    private String getCourseContext() {
        List<Resource> resources = resourceService.findByCourse(currentCourse.getId());
        StringBuilder context = new StringBuilder();
        com.edulink.gui.services.PdfExtractorService pdfExtractor = new com.edulink.gui.services.PdfExtractorService();
        for (Resource r : resources) {
            if ("PDF".equalsIgnoreCase(r.getType())) {
                context.append("Resource Title: ").append(r.getTitle()).append("\n");
                context.append(pdfExtractor.extractText(r.getUrl())).append("\n\n");
            }
        }
        return context.toString();
    }

    private void handleGenerateSummary(Resource r) {
        String pdfText = new com.edulink.gui.services.PdfExtractorService().extractText(r.getUrl());
        if (pdfText == null || pdfText.trim().isEmpty() || pdfText.startsWith("Error") || pdfText.startsWith("File not found")) {
            com.edulink.gui.util.EduAlert.show(com.edulink.gui.util.EduAlert.AlertType.ERROR, "Extraction Failed", "Could not extract text from this PDF. It might be empty, a scanned image, or missing.");
            return;
        }
        String systemPrompt = "You are an AI assistant. Provide a concise, easy-to-understand summary of the following document.";
        showAiDialog("Summary: " + r.getTitle(), "Generating summary... Please wait.", systemPrompt, pdfText);
    }

    private void handleGenerateQuiz(Resource r) {
        String pdfText = new com.edulink.gui.services.PdfExtractorService().extractText(r.getUrl());
        if (pdfText == null || pdfText.trim().isEmpty() || pdfText.startsWith("Error") || pdfText.startsWith("File not found")) {
            com.edulink.gui.util.EduAlert.show(com.edulink.gui.util.EduAlert.AlertType.ERROR, "Extraction Failed", "Could not extract text from this PDF. It might be empty, a scanned image, or missing.");
            return;
        }
        String systemPrompt = "You are a teacher. Generate a 5-question multiple-choice quiz based on the provided document. "
            + "You MUST format your EXACT response strictly as follows for each question:\n\n"
            + "Q: [Question Text]\n"
            + "A) [Option A Text]\n"
            + "B) [Option B Text]\n"
            + "C) [Option C Text]\n"
            + "D) [Option D Text]\n"
            + "ANSWER: [A, B, C, or D]\n\n"
            + "Separate each question with exactly '---' on a new line. Do not include any intro or outro text.";
        showInteractiveQuizDialog("Quiz: " + r.getTitle(), systemPrompt, pdfText);
    }

    private void showInteractiveQuizDialog(String title, String systemPrompt, String content) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle(title);
        
        VBox root = new VBox(15);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        Label loadingLabel = new Label("Generating quiz... Please wait.");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1a1a2e;");
        
        VBox quizContainer = new VBox(20);
        quizContainer.setStyle("-fx-background-color: #1a1a2e;");
        scrollPane.setContent(loadingLabel);
        
        root.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        dialog.setScene(new javafx.scene.Scene(root, 650, 550));
        dialog.show();
        
        String finalContent = content.length() > 20000 ? content.substring(0, 20000) : content;
        
        new Thread(() -> {
            com.edulink.gui.services.GroqService groq = new com.edulink.gui.services.GroqService();
            String response = groq.generateResponse(systemPrompt, finalContent);
            
            javafx.application.Platform.runLater(() -> {
                quizContainer.getChildren().clear();
                
                String[] questions = response.split("---");
                for (String qBlock : questions) {
                    if (qBlock.trim().isEmpty()) continue;
                    
                    VBox qBox = new VBox(10);
                    qBox.setStyle("-fx-background-color: #2a2a3e; -fx-padding: 15; -fx-background-radius: 8;");
                    
                    String qText = extractField(qBlock, "Q:", "A)");
                    String optA = extractField(qBlock, "A)", "B)");
                    String optB = extractField(qBlock, "B)", "C)");
                    String optC = extractField(qBlock, "C)", "D)");
                    String optD = extractField(qBlock, "D)", "ANSWER:");
                    String answer = extractField(qBlock, "ANSWER:", null);
                    
                    if (qText.isEmpty() || answer.isEmpty()) {
                        continue;
                    }
                    
                    Label qLabel = new Label(qText.trim());
                    qLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-wrap-text: true; -fx-font-size: 14px;");
                    
                    ToggleGroup group = new ToggleGroup();
                    RadioButton rbA = createOption("A) " + optA.trim(), group);
                    RadioButton rbB = createOption("B) " + optB.trim(), group);
                    RadioButton rbC = createOption("C) " + optC.trim(), group);
                    RadioButton rbD = createOption("D) " + optD.trim(), group);
                    
                    Label resultLabel = new Label();
                    resultLabel.setStyle("-fx-font-weight: bold; -fx-wrap-text: true;");
                    resultLabel.setVisible(false);
                    resultLabel.setManaged(false);
                    
                    javafx.beans.value.ChangeListener<Toggle> listener = (obs, oldV, newV) -> {
                        if (newV != null) {
                            rbA.setDisable(true);
                            rbB.setDisable(true);
                            rbC.setDisable(true);
                            rbD.setDisable(true);
                            
                            RadioButton selected = (RadioButton) newV;
                            boolean isCorrect = selected.getText().startsWith(answer.trim().substring(0, 1));
                            
                            if (isCorrect) {
                                resultLabel.setText("✅ Correct!");
                                resultLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");
                            } else {
                                resultLabel.setText("❌ Incorrect. The correct answer is " + answer.trim());
                                resultLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                            }
                            resultLabel.setVisible(true);
                            resultLabel.setManaged(true);
                        }
                    };
                    group.selectedToggleProperty().addListener(listener);
                    
                    qBox.getChildren().addAll(qLabel, rbA, rbB, rbC, rbD, resultLabel);
                    quizContainer.getChildren().add(qBox);
                }
                
                if (quizContainer.getChildren().isEmpty()) {
                    Label errLabel = new Label("Failed to parse quiz format. Raw response:\n" + response);
                    errLabel.setStyle("-fx-text-fill: #ef4444; -fx-wrap-text: true;");
                    quizContainer.getChildren().add(errLabel);
                }
                
                scrollPane.setContent(quizContainer);
            });
        }).start();
    }
    
    private RadioButton createOption(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #a0a0ab; -fx-wrap-text: true; -fx-font-size: 13px;");
        return rb;
    }
    
    private String extractField(String source, String startTag, String endTag) {
        int startIndex = source.indexOf(startTag);
        if (startIndex == -1) return "";
        startIndex += startTag.length();
        
        int endIndex = endTag != null ? source.indexOf(endTag, startIndex) : source.length();
        if (endIndex == -1) endIndex = source.length();
        
        return source.substring(startIndex, endIndex).trim();
    }

    private void showAiDialog(String title, String initialMsg, String systemPrompt, String content) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle(title);
        
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(15));
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        TextArea textArea = new TextArea(initialMsg);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-control-inner-background: #2a2a3e; -fx-text-fill: white;");
        textArea.setPrefSize(500, 400);
        
        root.getChildren().add(textArea);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.show();
        
        String finalContent = content.length() > 20000 ? content.substring(0, 20000) : content;
        
        new Thread(() -> {
            com.edulink.gui.services.GroqService groq = new com.edulink.gui.services.GroqService();
            String response = groq.generateResponse(systemPrompt, finalContent);
            javafx.application.Platform.runLater(() -> {
                textArea.setText(response);
            });
        }).start();
    }

    @FXML
    private void handleAskAi() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("AI Assistant - " + currentCourse.getTitle());
        
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(15));
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setPrefWidth(500);
        scrollPane.setStyle("-fx-background: #1a1a2e; -fx-border-color: #2a2a3e; -fx-border-radius: 5;");
        
        VBox chatContainer = new VBox(15);
        chatContainer.setPadding(new javafx.geometry.Insets(10));
        chatContainer.setStyle("-fx-background-color: transparent;");
        scrollPane.setContent(chatContainer);
        
        // Initial AI message
        addChatMessage(chatContainer, "AI", "Hello! I am here to answer questions strictly about the course '" + currentCourse.getTitle() + "'. What would you like to know?", true);
        
        TextField inputField = new TextField();
        inputField.setPromptText("Ask a question...");
        inputField.setStyle("-fx-background-color: #2a2a3e; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 6;");
        
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        
        HBox inputBox = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        
        root.getChildren().addAll(scrollPane, inputBox);
        
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.show();
        
        new Thread(() -> {
            String context = getCourseContext();
            if (context.trim().isEmpty()) {
                context = "[No readable PDF resources found for this course. Please rely on general knowledge regarding the course topic.]";
            } else if (context.length() > 20000) {
                context = context.substring(0, 20000);
            }
            final String finalContext = context;
            
            javafx.application.Platform.runLater(() -> {
                sendBtn.setOnAction(e -> {
                    String question = inputField.getText().trim();
                    if (question.isEmpty()) return;
                    
                    addChatMessage(chatContainer, "You", question, false);
                    inputField.clear();
                    sendBtn.setDisable(true);
                    
                    // Auto-scroll to bottom
                    javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
                    
                    new Thread(() -> {
                        com.edulink.gui.services.GroqService groq = new com.edulink.gui.services.GroqService();
                        String systemPrompt = "You are a helpful teaching assistant for the course '" + currentCourse.getTitle() + "'. "
                                + "Your primary source of knowledge is the following course content. "
                                + "If the user greets you, greet them back and ask how you can help with the course. "
                                + "If the user asks a question completely unrelated to this course content (e.g. general knowledge or another subject), respond exactly with: "
                                + "'This chatbot is limited to the " + currentCourse.getTitle() + " course resources. Please ask a question related to this course.'\n\n"
                                + "COURSE CONTENT:\n" + finalContext;
                        
                        String response = groq.generateResponse(systemPrompt, question);
                        
                        javafx.application.Platform.runLater(() -> {
                            addChatMessage(chatContainer, "AI", response, true);
                            sendBtn.setDisable(false);
                            // Auto-scroll to bottom
                            javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
                        });
                    }).start();
                });
            });
        }).start();
    }

    private void addChatMessage(VBox container, String sender, String message, boolean isAi) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isAi ? javafx.geometry.Pos.TOP_LEFT : javafx.geometry.Pos.TOP_RIGHT);
        
        Label iconLabel = new Label(isAi ? "🤖" : "👤");
        iconLabel.setStyle("-fx-font-size: 24px;");
        
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setPadding(new javafx.geometry.Insets(10));
        
        if (isAi) {
            messageLabel.setStyle("-fx-background-color: #2a2a3e; -fx-text-fill: white; -fx-background-radius: 0 12 12 12;");
            messageBox.getChildren().addAll(iconLabel, messageLabel);
        } else {
            messageLabel.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-background-radius: 12 0 12 12;");
            messageBox.getChildren().addAll(messageLabel, iconLabel);
        }
        
        container.getChildren().add(messageBox);
    }
}
