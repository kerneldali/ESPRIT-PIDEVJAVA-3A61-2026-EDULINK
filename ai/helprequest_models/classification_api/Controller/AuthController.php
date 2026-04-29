<?php

namespace App\Controller;

use App\Entity\User;
use App\Form\RegistrationFormType;
use App\Service\RecaptchaService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;
use Psr\Log\LoggerInterface;

class AuthController extends AbstractController
{
    public function __construct(
        private string $recaptchaSiteKey,
        private string $mailerFrom,
        private RecaptchaService $recaptchaService,
    ) {
    }

    #[Route('/login', name: 'login')]
    public function login(AuthenticationUtils $authenticationUtils): Response
    {
        if ($this->getUser()) {
            if (in_array('ROLE_ADMIN', $this->getUser()->getRoles())) {
                return $this->redirectToRoute('admin_dashboard');
            }
            return $this->redirectToRoute('student_dashboard');
        }

        $error = $authenticationUtils->getLastAuthenticationError();
        $lastUsername = $authenticationUtils->getLastUsername();

        return $this->render('auth/login.html.twig', [
            'last_username' => $lastUsername,
            'error' => $error,
            'recaptcha_site_key' => $this->recaptchaSiteKey,
        ]);
    }

    #[Route('/logout', name: 'app_logout')]
    public function logout(): void
    {
        throw new \LogicException('This method can be blank - it will be intercepted by the logout key on your firewall.');
    }

    #[Route('/register', name: 'register')]
    public function register(
        Request $request,
        UserPasswordHasherInterface $userPasswordHasher,
        EntityManagerInterface $entityManager,
        MailerInterface $mailer,
        LoggerInterface $logger,
    ): Response {
        $user = new User();
        $form = $this->createForm(RegistrationFormType::class, $user);
        $form->handleRequest($request);

        $recaptchaError = null;

        if ($form->isSubmitted() && $form->isValid()) {
            // Validate reCAPTCHA
            $recaptchaResponse = (string) $request->request->get('g-recaptcha-response');
            if (!$this->recaptchaService->verify($recaptchaResponse)) {
                $recaptchaError = 'Please complete the reCAPTCHA verification.';
            } else {
                // Check for existing user with same email
                $existingUser = $entityManager->getRepository(User::class)->findOneBy(['email' => $user->getEmail()]);
                if ($existingUser) {
                    if ($existingUser->isVerified()) {
                        $recaptchaError = 'An account with this email already exists. Please log in.';
                    } else {
                        $entityManager->remove($existingUser);
                        $entityManager->flush();
                    }
                }

                if (!$recaptchaError) {
                    $user->setPassword(
                        $userPasswordHasher->hashPassword($user, $form->get('plainPassword')->getData())
                    );
                    $user->setRoles(['ROLE_STUDENT']);
                    $user->setIsVerified(false);

                    $otp = (string) random_int(100000, 999999);
                    $user->setResetOtp($otp);
                    $user->setResetOtpExpiresAt(new \DateTime('+15 minutes'));

                    $entityManager->persist($user);
                    $entityManager->flush();

                    // Send verification email
                    $emailSent = false;
                    try {
                        $emailMessage = (new Email())
                            ->from($this->mailerFrom)
                            ->to((string) $user->getEmail())
                            ->subject('EduLink - Verify Your Email')
                            ->html("
                                <div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 2rem; text-align: center;'>
                                    <h2 style='color: #6d28d9;'>Welcome to EduLink! 🎓</h2>
                                    <p>Your verification code is:</p>
                                    <div style='background: #f3f4f6; padding: 1rem; border-radius: 12px; font-size: 2rem; font-weight: 700; letter-spacing: 8px; color: #6d28d9; margin: 1rem 0;'>$otp</div>
                                    <p style='color: #6b7280; font-size: 0.9rem;'>This code expires in 15 minutes.</p>
                                </div>
                            ");
                        $mailer->send($emailMessage);
                        $emailSent = true;
                        $logger->info("Registration OTP sent to {$user->getEmail()}");
                    } catch (\Exception $e) {
                        $logger->error("Failed to send verification email: " . $e->getMessage());
                    }

                    $request->getSession()->set('verify_email', $user->getEmail());

                    if ($emailSent) {
                        $this->addFlash('success', 'Account created! Check your email for the verification code.');
                    } else {
                        $this->addFlash('warning', 'Email could not be sent. Please try resending the code.');
                    }
                    return $this->redirectToRoute('verify_registration');
                }
            }
        }

        return $this->render('auth/register.html.twig', [
            'registrationForm' => $form->createView(),
            'recaptcha_site_key' => $this->recaptchaSiteKey,
            'recaptcha_error' => $recaptchaError,
        ]);
    }

    #[Route('/verify-email', name: 'verify_registration')]
    public function verifyRegistration(Request $request, EntityManagerInterface $entityManager): Response
    {
        $email = $request->getSession()->get('verify_email');
        if (!$email) {
            return $this->redirectToRoute('register');
        }

        $error = null;

        if ($request->isMethod('POST')) {
            $otp = trim((string) $request->request->get('otp', ''));
            $user = $entityManager->getRepository(User::class)->findOneBy(['email' => $email]);

            if (!$user) {
                $error = 'Account not found.';
            } elseif (empty($otp)) {
                $error = 'Please enter the verification code.';
            } elseif ($user->getResetOtp() !== $otp) {
                $error = 'Invalid verification code.';
            } elseif ($user->getResetOtpExpiresAt() < new \DateTime()) {
                $error = 'Code expired. Please register again.';
            } else {
                $user->setIsVerified(true);
                $user->setResetOtp(null);
                $user->setResetOtpExpiresAt(null);
                $entityManager->flush();

                $this->addFlash('success', 'Email verified! You can now log in.');
                $request->getSession()->remove('verify_email');
                return $this->redirectToRoute('login');
            }
        }

        return $this->render('auth/verify_registration.html.twig', [
            'email' => $email,
            'error' => $error,
        ]);
    }

    #[Route('/verify-email/resend', name: 'resend_verification_otp', methods: ['POST'])]
    public function resendOtp(Request $request, EntityManagerInterface $entityManager, MailerInterface $mailer, LoggerInterface $logger): Response
    {
        $email = $request->getSession()->get('verify_email');
        if (!$email) {
            return $this->redirectToRoute('register');
        }

        $user = $entityManager->getRepository(User::class)->findOneBy(['email' => $email]);
        if (!$user) {
            return $this->redirectToRoute('register');
        }

        $otp = (string) random_int(100000, 999999);
        $user->setResetOtp($otp);
        $user->setResetOtpExpiresAt(new \DateTime('+15 minutes'));
        $entityManager->flush();

        try {
            $emailMessage = (new Email())
                ->from($this->mailerFrom)
                ->to((string) $user->getEmail())
                ->subject('EduLink - New Verification Code')
                ->html("
                    <div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 2rem; text-align: center;'>
                        <h2 style='color: #6d28d9;'>New Verification Code 🔐</h2>
                        <div style='background: #f3f4f6; padding: 1rem; border-radius: 12px; font-size: 2rem; font-weight: 700; letter-spacing: 8px; color: #6d28d9; margin: 1rem 0;'>$otp</div>
                        <p style='color: #6b7280; font-size: 0.9rem;'>This code expires in 15 minutes.</p>
                    </div>
                ");

            $mailer->send($emailMessage);
            $this->addFlash('success', 'New verification code sent!');
        } catch (\Exception $e) {
            $logger->error("Failed to resend OTP: " . $e->getMessage());
            $this->addFlash('error', 'Failed to send email. Please try again.');
        }

        return $this->redirectToRoute('verify_registration');
    }

    #[Route('/forgot-password', name: 'forgot_password')]
    public function forgotPassword(): Response
    {
        return $this->render('auth/forgot_password.html.twig');
    }
}