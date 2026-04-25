import math
import re
from collections import Counter

class SentimentModel:
    def __init__(self):
        self.positive_word_counts = Counter()
        self.negative_word_counts = Counter()
        self.positive_count = 0
        self.negative_count = 0
        self.vocab = set()

    def tokenize(self, text):
        return re.findall(r'\w+', text.lower())

    def train(self, texts, labels):
        for text, label in zip(texts, labels):
            words = self.tokenize(text)
            if label == 'positive':
                self.positive_count += 1
                self.positive_word_counts.update(words)
            else:
                self.negative_count += 1
                self.negative_word_counts.update(words)
            self.vocab.update(words)

    def predict(self, text):
        words = self.tokenize(text)
        
        # Log probabilities to avoid underflow
        total_samples = self.positive_count + self.negative_count
        if total_samples == 0: return "neutral"
        
        pos_score = math.log(self.positive_count / total_samples)
        neg_score = math.log(self.negative_count / total_samples)
        
        vocab_size = len(self.vocab)
        pos_total_words = sum(self.positive_word_counts.values())
        neg_total_words = sum(self.negative_word_counts.values())
        
        for word in words:
            # Add-one smoothing (Laplace)
            pos_score += math.log((self.positive_word_counts[word] + 1) / (pos_total_words + vocab_size))
            neg_score += math.log((self.negative_word_counts[word] + 1) / (neg_total_words + vocab_size))
            
        return "positive" if pos_score > neg_score else "negative"

# Training data
training_data = [
    ("I love this learning platform", "positive"),
    ("Today was a productive day", "positive"),
    ("I am very happy with my progress", "positive"),
    ("Great achievements today", "positive"),
    ("Feeling motivated and excited", "positive"),
    ("Successful task completion", "positive"),
    ("I am feeling sad and overwhelmed", "negative"),
    ("This day was terrible", "negative"),
    ("I hate failing at my goals", "negative"),
    ("Feeling stressed and anxious", "negative"),
    ("The progress is too slow and frustrating", "negative"),
    ("I want to give up today", "negative")
]

# Initialize and train
model = SentimentModel()
texts, labels = zip(*training_data)
model.train(texts, labels)

if __name__ == "__main__":
    # Test
    print(model.predict("I am feeling great today"))
    print(model.predict("I am feeling overwhelmed"))
