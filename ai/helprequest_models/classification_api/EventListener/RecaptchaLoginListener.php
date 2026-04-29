<?php

namespace App\EventListener;

use App\Service\RecaptchaService;
use Symfony\Component\EventDispatcher\Attribute\AsEventListener;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException;
use Symfony\Component\Security\Http\Event\CheckPassportEvent;

#[AsEventListener(event: CheckPassportEvent::class, priority: 512)]
class RecaptchaLoginListener
{
    public function __construct(
        private RecaptchaService $recaptchaService,
        private RequestStack $requestStack,
    ) {
    }

    public function __invoke(CheckPassportEvent $event): void
    {
        $request = $this->requestStack->getCurrentRequest();
        if (!$request || !$request->isMethod('POST')) {
            return;
        }

        $recaptchaResponse = $request->request->get('g-recaptcha-response');

        if (!$this->recaptchaService->verify($recaptchaResponse)) {
            throw new CustomUserMessageAuthenticationException('Please complete the reCAPTCHA verification.');
        }
    }
}
