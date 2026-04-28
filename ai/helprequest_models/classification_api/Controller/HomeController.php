<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class HomeController extends AbstractController
{
    #[Route('/', name: 'home')]
    public function index(): Response
    {
        $this->addFlash('success', 'Welcome to EduLink! Experience the future of AI-powered learning.');
        return $this->render('home/index.html.twig');
    }
}
