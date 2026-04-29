<?php

namespace App\Service;

use Symfony\Component\HttpClient\HttpClient;

class RecaptchaService
{
    private string $secretKey;

    public function __construct(string $recaptchaSecretKey)
    {
        $this->secretKey = $recaptchaSecretKey;
    }

    public function verify(?string $recaptchaResponse): bool
    {
        if (empty($recaptchaResponse)) {
            return false;
        }

        $client = HttpClient::create();
        $response = $client->request('POST', 'https://www.google.com/recaptcha/api/siteverify', [
            'body' => [
                'secret' => $this->secretKey,
                'response' => $recaptchaResponse,
            ],
        ]);

        $data = $response->toArray(false);

        return ($data['success'] ?? false) === true;
    }
}
