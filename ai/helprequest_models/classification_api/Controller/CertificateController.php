<?php

namespace App\Controller;

use App\Entity\Cours;
use App\Entity\Enrollment;
use App\Repository\EnrollmentRepository;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/student/course')]
class CertificateController extends AbstractController
{
    #[Route('/{id}/certificate', name: 'app_student_course_certificate')]
    public function generate(Cours $cours, EnrollmentRepository $enrollmentRepo): Response
    {
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        $enrollment = $enrollmentRepo->findOneBy(['student' => $user, 'cours' => $cours]);

        if (!$enrollment || $enrollment->getProgress() < 100) {
            $this->addFlash('error', 'Complete the course first!');
            return $this->redirectToRoute('app_student_course_detail', ['id' => $cours->getId()]);
        }

        // Generate a unique Certificate ID
        $certId = sprintf('CERT-%d-%d-%d', $user->getId(), $cours->getId(), bin2hex(random_bytes(4)));
        $date = new \DateTimeImmutable();

        // DomPDF setup
        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->set('isRemoteEnabled', true);

        $dompdf = new Dompdf($pdfOptions);

        $html = $this->renderView('student/certificate.html.twig', [
            'user' => $user,
            'cours' => $cours,
            'certId' => $certId,
            'date' => $date
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'Certificate-' . $cours->getTitle() . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"',
        ]);
    }
}
