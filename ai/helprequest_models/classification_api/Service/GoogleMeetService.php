<?php

namespace App\Service;

class GoogleMeetService
{
    /**
     * Generate a Google Meet-style link for online events.
     * Uses a hash-based approach to produce a realistic meet.google.com URL.
     */
    public function generateMeetLink(string $eventTitle): string
    {
        $seed = $eventTitle . '-' . microtime(true) . '-' . random_int(1000, 9999);
        $hash = md5($seed);

        // Google Meet format: xxx-xxxx-xxx (lowercase letters)
        $code = $this->hashToMeetCode($hash);

        return 'https://meet.google.com/' . $code;
    }

    private function hashToMeetCode(string $hash): string
    {
        $letters = '';
        for ($i = 0; $i < strlen($hash); $i++) {
            $char = $hash[$i];
            if (ctype_alpha($char)) {
                $letters .= strtolower($char);
            } else {
                $letters .= chr(ord('a') + (hexdec($char) % 26));
            }
        }

        // Format: xxx-xxxx-xxx
        $part1 = substr($letters, 0, 3);
        $part2 = substr($letters, 3, 4);
        $part3 = substr($letters, 7, 3);

        return $part1 . '-' . $part2 . '-' . $part3;
    }
}
