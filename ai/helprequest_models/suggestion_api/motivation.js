/**
 * Motivation Coach Voice System
 * Uses Web Speech API to read messages from AI
 */
const MotivationCoach = {
    speak: async function (text) {
        try {
            // Appeler notre microservice local pour générer l'audio HT (edge-tts)
            const response = await fetch('http://localhost:8000/voice', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: text })
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = URL.createObjectURL(blob);
                const audio = new Audio(url);
                audio.play();
            } else {
                throw new Error("Erreur microservice voix");
            }
        } catch (e) {
            console.warn("Fallback sur la voix système car le microservice voix est injoignable.");
            // Fallback sur la voix système si le microservice est éteint
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'fr-FR';
            window.speechSynthesis.speak(utterance);
        }
    },

    boostMe: async function (userChallengeId) {
        try {
            const button = event.currentTarget;
            const originalText = button.innerHTML;
            button.disabled = true;
            button.innerHTML = "🌀...";

            // Ajouter un timestamp pour éviter le cache du navigateur
            const response = await fetch(`/admin/ai/motivation/${userChallengeId}?t=${new Date().getTime()}`);
            const data = await response.json();

            if (data.message) {
                this.speak(data.message);
                console.log("Coach dit: " + data.message);
            }

            button.disabled = false;
            button.innerHTML = originalText;
        } catch (error) {
            console.error("Erreur motivation:", error);
            const localFallbacks = [
                "Allez champion, ne lâche rien !",
                "Tu es capable de grandes choses, continue !",
                "L'effort d'aujourd'hui est le succès de demain !",
                "Reste concentré, tu y es presque !",
                "Chaque petite victoire compte, bravo !"
            ];
            this.speak(localFallbacks[Math.floor(Math.random() * localFallbacks.length)]);

            const button = document.querySelector('button[onclick*="boostMe"]'); // Fallback button selection
            if (button) {
                button.disabled = false;
                button.innerHTML = "🚀 Boost me!";
            }
        }
    }
};

// Charger les voix au démarrage pour éviter le délai au premier clic
window.speechSynthesis.getVoices();
if (speechSynthesis.onvoiceschanged !== undefined) {
    speechSynthesis.onvoiceschanged = () => window.speechSynthesis.getVoices();
}
