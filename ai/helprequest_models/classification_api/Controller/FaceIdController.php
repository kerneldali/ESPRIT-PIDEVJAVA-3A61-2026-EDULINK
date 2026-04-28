<?php

namespace App\Controller;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Authentication\UserAuthenticatorInterface;
use Symfony\Component\Security\Http\Authenticator\FormLoginAuthenticator;

class FaceIdController extends AbstractController
{
    #[Route('/student/face-setup', name: 'student_face_setup')]
    public function setup(): Response
    {
        return $this->render('student/face_setup.html.twig');
    }

    #[Route('/api/face/register', name: 'api_face_register', methods: ['POST'])]
    public function registerFace(Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $user = $this->getUser();
        if (!$user) {
            return new JsonResponse(['error' => 'User not logged in'], 403);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['descriptor'])) {
            return new JsonResponse(['error' => 'No descriptor provided'], 400);
        }

        // We expect descriptor to be an array of floats
        /** @var User $user */
        $user->setFaceDescriptor($data['descriptor']);
        $entityManager->flush();

        return new JsonResponse(['success' => true]);
    }

    #[Route('/api/face/login', name: 'api_face_login', methods: ['POST'])]
    public function loginFace(
        Request $request, 
        EntityManagerInterface $entityManager,
        \Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface $tokenStorage,
        \Symfony\Component\HttpFoundation\RequestStack $requestStack
    ): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        if (!isset($data['descriptor'])) {
            return new JsonResponse(['error' => 'No descriptor provided'], 400);
        }

        $inputDescriptor = $data['descriptor'];
        $users = $entityManager->getRepository(User::class)->findAll();

        $bestMatchUser = null;
        $bestMatchDistance = 1.0; 

        foreach ($users as $user) {
            $storedDescriptor = $user->getFaceDescriptor();
            if ($storedDescriptor) {
                $distance = $this->euclideanDistance($inputDescriptor, $storedDescriptor);
                if ($distance < 0.6 && $distance < $bestMatchDistance) {
                    $bestMatchDistance = $distance;
                    $bestMatchUser = $user;
                }
            }
        }

        if ($bestMatchUser) {
            // Authenticate the user manually
            $token = new \Symfony\Component\Security\Core\Authentication\Token\UsernamePasswordToken(
                $bestMatchUser,
                'main', // Firewall name
                $bestMatchUser->getRoles()
            );
            
            $tokenStorage->setToken($token);
            
            $session = $requestStack->getSession();
            $session->set('_security_main', serialize($token));
            $session->save();

            $redirectRoute = in_array('ROLE_ADMIN', $bestMatchUser->getRoles()) ? 'admin_dashboard' : 'student_dashboard';

            return new JsonResponse([
                'success' => true, 
                'redirect' => $this->generateUrl($redirectRoute)
            ]);
        }

        return new JsonResponse(['error' => 'Face not recognized'], 401);
    }

    /**
     * @param array<int, float> $a
     * @param array<int, float> $b
     */
    private function euclideanDistance(array $a, array $b): float
    {
        if (count($a) !== count($b)) {
            return 100.0;
        }
        
        $sum = 0.0;
        for ($i = 0; $i < count($a); $i++) {
            $diff = $a[$i] - $b[$i];
            $sum += $diff * $diff;
        }
        return sqrt($sum);
    }
}
