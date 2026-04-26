<?php

namespace App\Controller\Api;

use App\Repository\CoursRepository;
use App\Repository\EventRepository;
use App\Repository\MatiereRepository;
use App\Service\GroqService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

class ChatbotController extends AbstractController
{
    private GroqService $aiService;
    private \App\Service\ContextService $contextService;
    private \App\Service\AiMicroservice $aiMicroservice;

    public function __construct(
        GroqService $aiService,
        \App\Service\ContextService $contextService,
        \App\Service\AiMicroservice $aiMicroservice
    ) {
        $this->aiService = $aiService;
        $this->contextService = $contextService;
        $this->aiMicroservice = $aiMicroservice;
    }

    #[Route('/api/chat', name: 'api_chat', methods: ['POST'])]
    public function chat(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $userMessage = $data['message'] ?? '';
        $history = $data['history'] ?? [];

        if (empty($userMessage)) {
            return new JsonResponse(['error' => 'Empty message'], 400);
        }

        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();

        // 1. TRY AGENT (LangChain Agent via Microservice)
        if ($user) {
            try {
                $agentResult = $this->aiMicroservice->chat((string) $userMessage, (int) $user->getId());
                // We consider it a success if we get a response that doesn't look like our custom error
                if (isset($agentResult['response']) && !str_contains($agentResult['response'], "trouble connecting to my brain")) {
                    return new JsonResponse(['reply' => $agentResult['response']]);
                }
            } catch (\Throwable $e) {
                // Ignore agent errors, fall back to Groq
                error_log('[Agent Fallback Triggered] ' . $e->getMessage());
            }
        }

        // 2. FALLBACK TO GROQ (Original logic)
        try {
            // Get User-specific context (Notes, Enrollments, Help Requests)
            $userInfo = $user ? $this->contextService->getUserContext($user) : "No user authenticated.";

            // Get Platform-wide context (Courses, Events, Categories)
            $platformContext = $this->contextService->getPlatformContext();

            $navigation = "HOW TO USE EDULINK (NAVIGATION GUIDE):\n";
            $navigation .= "- To study or see all courses: My Courses (/student/courses)\n";
            $navigation .= "- To use Study Tools (Summaries, Quizzes): Study Tools (/student/ai-tools)\n";
            $navigation .= "- To see points/journal: Journal or Wallet\n";
            $navigation .= "- To join a challenge: Challenges (/challenges)\n";
            $navigation .= "- To create an event: Events -> Create button (/creator/event/new)\n";
            $navigation .= "- To change profile: Profile\n";

            $historyText = "PREVIOUS CONVERSATION HISTORY:\n";
            $slicedHistory = array_slice($history, -6);
            foreach ($slicedHistory as $msg) {
                $role = (isset($msg['type']) && $msg['type'] === 'user') ? 'User' : 'Assistant';
                $text = $msg['text'] ?? '';
                if ($text)
                    $historyText .= "$role: " . $text . "\n";
            }

            $systemPrompt = "You are the EduLink Assistant, the expert guide for the EduLink Educational Platform. 
            
            $userInfo
            
            $platformContext
            
            $navigation
            
            $historyText
            
            Current Message: $userMessage
            
            STRICT INSTRUCTIONS:
            1. PERSONA: Answer as a helpful 'Study Assistant'. If the user asks 'What are my notes?' or 'How is my progress?', check the 'USER LIVE DATA' provided.
            2. CONTEXT AWARENESS: You have full access to the user's notes, enrollments, challenges, badges, reminders, personal tasks, transactions, and community posts. Reference them when relevant.
            3. NO AI BRANDING: Never mention GPT, Gemini, or being an AI. You are a 'Core System' feature of EduLink.
            4. MATCHMAKING: Suggest courses from the 'Available Courses' list if relevant.
            5. BADGES & XP: If asked about badges, reference the user's earned badges and explain what XP thresholds unlock next badges. XP and Wallet Balance are the same thing.
            6. CHALLENGES: If asked about challenges, reference the user's active challenges, their progress, and reward points.
            7. STYLE: Concise, encouraging, and professional. Match the user's language.";

            $reply = $this->aiService->generateContent($systemPrompt);

            return new JsonResponse(['reply' => $reply]);
        } catch (\Throwable $e) {
            error_log('[Chatbot Error] ' . get_class($e) . ': ' . $e->getMessage());

            $errorMsg = match (true) {
                str_contains($e->getMessage(), '401') => "AI service authentication failed.",
                str_contains($e->getMessage(), '429') => "AI service is temporarily overloaded.",
                str_contains($e->getMessage(), 'timed out') || str_contains($e->getMessage(), 'timeout') => "AI service took too long to respond.",
                default => "Technical Error: I'm currently unavailable.",
            };

            return new JsonResponse(['reply' => $errorMsg], 500);
        }
    }
}
