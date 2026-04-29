<?php

namespace App\Service;

use App\Entity\Event;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class PredictionService
{
    public function __construct(
        private HttpClientInterface $httpClient,
        private LoggerInterface $logger
    ) {
    }

    public function predictSuccess(Event $event): ?int
    {
        try {
            $formattedDate = $event->getDateStart() ? $event->getDateStart()->format('Y-m-d H:i:s') : null;
            $payload = [
                'title' => $event->getTitle(),
                'description' => $event->getDescription(),
                'dateStart' => $formattedDate,
                'maxCapacity' => $event->getMaxCapacity(),
            ];

            $this->logger->info('Sending prediction request in PredictionService', ['payload' => $payload]);

            $host = $_ENV['AI_SERVICES_HOST'] ?? getenv('AI_SERVICES_HOST') ?: '127.0.0.1';
            $response = $this->httpClient->request('POST', 'http://' . $host . ':5000/predict', [
                'json' => $payload,
                'timeout' => 3, // Short timeout to not block Symfony if Flask is down
            ]);

            $statusCode = $response->getStatusCode();
            $this->logger->info('Received prediction response', ['status_code' => $statusCode]);

            if ($statusCode === 200) {
                $data = $response->toArray();
                $this->logger->info('Prediction data parsed', ['data' => $data]);
                return isset($data['predicted_target']) ? (int) $data['predicted_target'] : null;
            } else {
                $this->logger->warning('Prediction failed with non-200 status', ['content' => $response->getContent(false)]);
            }
        } catch (\Exception $e) {
            $this->logger->error('PredictionService exception: ' . $e->getMessage());
        }

        return null; // Return null if prediction fails
    }
}
