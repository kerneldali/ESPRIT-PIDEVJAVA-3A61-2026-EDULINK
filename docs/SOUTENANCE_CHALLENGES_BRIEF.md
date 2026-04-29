# Brief Soutenance — Module Challenges (EduLink)

> **À lire 1h avant la soutenance.** Lis chaque question à voix haute, formule ta réponse sans regarder, puis compare. Si tu hésites > 5s → revois.

---

## 1. Pitch en 30 secondes (à mémoriser)

> « Mon module gère le cycle de vie complet des **challenges gamifiés** d'EduLink : un enseignant les crée (avec assistance IA pour le contenu et l'image de couverture), les étudiants les rejoignent, soumettent leur travail, sont validés par l'enseignant, gagnent de l'XP, et reçoivent un certificat PDF avec QR code de vérification. Une couche de statistiques transforme les données d'engagement en **décisions automatiques** : le challenge le plus populaire est mis en avant, les sous-performants reçoivent un boost d'XP. »

3 mots-clés : **gamification, IA, data-driven**.

---

## 2. Architecture — comment le justifier

### Question : « Quelle est l'architecture de ton module ? »

**Réponse :**
3 couches strictes, MVC enrichi.

- **Modèle** (`models/challenge/`) : POJO purs (`Challenge`, `ChallengeTask`, `ChallengeParticipation`, `ChallengeSubmission`). Pas de logique, pas de JDBC.
- **Service** (`services/challenge/`) : 5 services. Un par agrégat (`ChallengeService`, `ChallengeTaskService`, `ChallengeParticipationService`, `ChallengeSubmissionService`, `CertificateService`) + 2 services techniques (`AIChallengeGenerator`, `ChallengeImageService`). Tous implémentent `IService<T>` → contrat homogène.
- **Contrôleur** (`controllers/challenge/`) : 1 controller par vue FXML. Aucune requête SQL dans les controllers.

**Pourquoi cette séparation ?** Si demain on remplace MySQL par une API REST, je ne touche que la couche Service. Les controllers et les FXML restent intacts.

### Piège classique : « Pourquoi pas un ORM (Hibernate/JPA) ? »

**Réponse honnête :**
Pour ce projet, JDBC pur a été choisi pour 3 raisons :
1. **Maîtrise totale du SQL** — utile en pédagogie, on voit ce qui se passe.
2. **Aucun mapping magique** — les bugs sont au niveau du `mapResultSet` qu'on contrôle ligne par ligne.
3. **Démarrage instantané** — pas de configuration JPA, pas de `persistence.xml`.

**Ce que je reconnais comme limite :** dans un vrai SaaS, JPA + Hibernate me ferait gagner 30% de code répétitif. Mais ce serait au prix d'une couche d'abstraction supplémentaire. **C'est un trade-off conscient, pas un oubli.**

---

## 3. Les 15 questions les plus probables

### Q1 — « Comment tu gères la connexion à la base ? »

- **Singleton** `MyConnection` qui charge la config depuis `src/main/resources/db.properties`.
- Le fichier **n'est pas versionné** (gitignore) — chaque dev a sa config locale.
- Un `db.properties.example` est versionné comme template.
- Au premier lancement, si `db.properties` n'existe pas, il est auto-créé à partir de l'exemple.
- Méthode `reconnect()` permet de forcer une reconnexion si la connexion a chuté.

**Pourquoi c'est bien :** mon coéquipier sur Windows a XAMPP sur le port 3306, moi sur macOS port 3307. Avant ce refactoring, on devait modifier `MyConnection.java` pour collaborer → conflit Git à chaque commit. Maintenant c'est zéro friction.

### Q2 — « Pourquoi il y a un fichier `db.properties.example` ? C'est juste de la doc ? »

Non. C'est le **template d'auto-bootstrap**. Si un nouveau dev clone le repo, le `db.properties` local n'existe pas. Au premier démarrage, `MyConnection` détecte l'absence et copie `.example` vers le local. Le dev modifie ensuite ses credentials sans toucher au repo. C'est **convention over configuration**.

### Q3 — « Comment tu sais quelle XP donner à quel challenge ? »

- Saisie manuelle par l'enseignant dans le formulaire.
- L'IA suggère un montant cohérent avec la difficulté (`EASY: 50-100`, `MEDIUM: 100-200`, `HARD: 200-300`).
- À l'attribution finale, c'est `getEffectiveXpReward()` qui est utilisé : base + boost actif.

### Q4 — « C'est quoi le boost d'XP ? »

Mécanisme de **rééquilibrage automatique**. Si la stat montre qu'un challenge a peu de participants, le système lui ajoute +50% d'XP pour 7 jours, ce qui le rend plus attractif. C'est une décision **prise par les données, pas par l'admin**. Voir `ChallengeService.runAutoDecisions()`.

**Algorithme** (à expliquer si demandé) :
1. Pour chaque challenge ouvert : score = `nb_participants × (taux_complétion + 1)`.
2. Score le plus haut → flagué FEATURED (badge or, ordre prioritaire dans la liste).
3. Tout challenge avec `participants < moyenne` → boost +50% pendant 7 jours.

Toute cette opération est **transactionnelle** (rollback en cas d'erreur).

### Q5 — « Pourquoi `score = participants × (rate + 1)` et pas juste `rate` ? »

Le `+1` empêche un challenge tout neuf (0 complétions) d'avoir un score de 0. Sinon il ne pourrait jamais devenir featured malgré un fort engagement initial. C'est une **régularisation à la naïve Bayes**.

### Q6 — « Comment tu gères les soumissions ? »

3 états : `PENDING` (étudiant a soumis), `VALIDATED` (prof a accepté → XP + certificat), `REJECTED` (prof a refusé → l'étudiant peut re-soumettre).

Côté DB : table `challenge_submission` avec FK vers `challenge_participation`. Pas de FK directe vers `user` — on passe toujours par la participation (intégrité).

### Q7 — « Le certificat, c'est quoi techniquement ? »

PDF généré avec **iText 7.2.5**.
- Background blanc, accents de couleur (vert/violet/or selon la difficulté).
- Footer avec **QR code généré par appel à l'API REST QR Server** (`api.qrserver.com`). ZXing 3.5.2 reste embarqué comme fallback si l'API est down.
- L'ID du certificat est un **hash SHA-256** déterministe basé sur `studentName+challengeTitle` → reproductible, le même couple regénère toujours le même ID.
- Stocké sur le Desktop de l'utilisateur (chemin lisible pour la démo).

### Q8 — « Pourquoi un QR code et qu'est-ce qu'il contient ? »

**Le QR encode l'URL d'une page de vérification statique** hébergée sur GitHub Pages, avec toutes les métadonnées du certificat en query string :

```
https://ali-belhadjali.github.io/edulink-verify/?id=EDU-A1B2C3D4E5&name=Ali%20Belhadj%20Ali&challenge=Java%20Collections&difficulty=MEDIUM&xp=150&date=2026-04-29
```

Quand on scanne avec un téléphone → la page s'ouvre, affiche un certificat colorisé avec badge ✓, infos du recipient, etc.

**Architecture délibérément minimaliste :**
1. **Page 100% statique** (HTML+JS, aucun backend, aucune DB) — déployée gratuitement sur GitHub Pages.
2. **Toute l'info vient du QR** — la page lit `window.location.search` et rend ce qu'elle voit. Aucun appel réseau.
3. **L'ID est cryptographique** — hash SHA-256 déterministe basé sur `nom + challenge`. Si quelqu'un modifie l'URL pour falsifier le nom, l'ID ne correspondra plus → détectable.
4. **Pas de mensonge** — on ne prétend pas avoir un PKI ni un registre officiel. C'est de la vérification *transparent ID-based*, pas un audit notarié.

**Argument sur la scalabilité :** *« Ce design est volontairement stateless. Si demain on veut un vrai registre, on ajoute un endpoint qui prend l'ID et confirme. La page existante n'a pas besoin d'être modifiée. »*

### Q8b — « Pourquoi une API externe pour le QR alors que ZXing fait pareil en local ? »

Trois raisons :
1. **Exigence pédagogique** — le projet doit consommer au moins une API REST externe.
2. **Découplage** — le binaire ne dépend plus d'une dépendance lourde pour cette feature.
3. **Pattern microservice** — c'est l'approche qu'on adopterait dans un système distribué : un service dédié à la génération QR, scalable indépendamment.

**Robustesse** : si l'API est lente/down, on tombe automatiquement sur ZXing (fallback). Le certificat est **toujours** émis. Voir `CertificateService.fetchQrBytes()`.

### Q9 — « L'IA, c'est ChatGPT ? Tu envoies les données des étudiants ? »

Non.
- LLM utilisé : **Groq llama-3.1-8b-instant** (open-source, hébergé chez Groq).
- Aucune donnée étudiante envoyée. Seul l'enseignant fournit un **sujet libre** (ex: "Java collections").
- Le LLM retourne un JSON structuré (titre, description, difficulté, XP, deadline, tâches).
- Le JSON est **validé et clampé** côté Java avant insertion (XP ∈ [10..500], difficulté ∈ {EASY, MEDIUM, HARD}).
- Si le LLM hallucine ou retourne du texte brut, on tombe en mode dégradé avec un message d'erreur clair.

### Q10 — « Et l'image générée par IA ? »

- Service : **Pollinations.ai** (Stable Diffusion gratuit, sans clé API).
- URL construite dans `ChallengeImageService.buildImageUrl(title, difficulty)`.
- Seed déterministe basé sur le hash du titre → **même titre = même image** (cache-friendly).
- Prompt cohérent (style "modern flat illustration, no text, academic theme") pour une identité visuelle homogène.
- Stockée comme URL dans la colonne `image_url`. JavaFX la charge en background avec `new Image(url, ..., true)` — pas de blocage UI.

### Q11 — « Et si Pollinations tombe ou est lent ? »

- Background loading : la card s'affiche immédiatement, l'image arrive après.
- Si l'image échoue à charger, le bloc image est silencieusement omis (catch). La card reste fonctionnelle.
- Pas de cache local pour l'instant — c'est une amélioration future (download → file cache).

### Q12 — « Stats → décision automatique : c'est juste cosmétique ou ça change vraiment quelque chose ? »

**Ça change vraiment.**
- L'XP donnée à la validation est `getEffectiveXpReward()` qui inclut le boost.
- Le tri de la liste publique est `ORDER BY featured DESC, deadline ASC` → le challenge featured remonte en haut.
- Le badge FEATURED a une couleur or distinctive et une bordure plus épaisse.

C'est ce qui distingue **stats actionable** vs **stats vitrine**.

### Q13 — « Tu as des tests ? »

Honnêteté : **non, pas de tests automatisés sur ce module**. À reconnaître ouvertement.

**Mitigation que tu as faite :**
- Validation des entrées dans la couche service (clamp XP, whitelist difficulté).
- Migrations idempotentes (`ADD COLUMN IF NOT EXISTS`) → relancer 10 fois ne casse rien.
- Transactions sur `runAutoDecisions()` → rollback en cas d'erreur partielle.

**Si on me pousse :** "Si je devais ajouter des tests demain, je commencerais par `ChallengeService.runAutoDecisions()` car c'est la logique métier la plus complexe. Je ferais un test unitaire avec une H2 in-memory et 3 challenges fictifs."

### Q14 — « Pourquoi pas un système de notifications quand on gagne de l'XP ? »

C'est dans le module Wallet/Notification qui n'est pas le mien. **Ne pas inventer**, dire : *« C'est géré par le module XP/Wallet de mon coéquipier. Mon module se contente d'incrémenter l'XP via `UserService` et de générer le certificat. »*

### Q15 — « Si demain tu voulais scaler à 100 000 utilisateurs, qu'est-ce qui casserait en premier ? »

Réponse stratégique (montre que tu réfléchis au-delà du PFA) :

1. **JDBC singleton** : une seule connexion partagée → goulot. À remplacer par un pool (HikariCP).
2. **Pollinations gratuit** : rate-limit possible. Il faudrait un cache CDN local des images.
3. **Pas de pagination** : `getAll()` ramène tous les challenges en mémoire. À paginer.
4. **PDFs sur disque local** : non-portable en prod. À mettre sur S3 ou équivalent.
5. **`runAutoDecisions()` synchrone** : à passer en cron asynchrone (Quartz ou similaire).

**Ce que tu démontres en disant ça :** tu sais que ton code n'est pas production-ready, et tu connais exactement les 5 fronts à attaquer pour qu'il le devienne. C'est mieux que de prétendre que c'est déjà parfait.

---

## 4. Les 5 questions piège à anticiper

### P1 — « Pourquoi tu utilises `featured` comme colonne et pas une table séparée `featured_challenge` ? »

Choix conscient. Une seule entrée FEATURED à la fois (par règle métier). Une colonne booléenne suffit, pas besoin d'une table 1-1. Ajouter une table serait de la **sur-ingénierie**.

### P2 — « Tu as un singleton sur `MyConnection`, c'est un anti-pattern ? »

Reconnaître la critique. Le singleton est effectivement controversé. **Ce que ça permet :** une seule connexion ouverte pour toute l'app, pas de fuite. **La limite :** non-thread-safe en l'état. Si on faisait des requêtes en parallèle, il faudrait soit un pool, soit synchroniser. Pour l'usage actuel (une UI mono-threadée), c'est suffisant.

### P3 — « Les FXML, c'est généré ou écrit à la main ? »

Écrit à la main, sans Scene Builder. **Pourquoi :** plus de contrôle sur la lisibilité du XML, et le merge Git est plus propre que les fichiers Scene Builder qui réordonnent les attributs.

### P4 — « Si l'IA génère un challenge avec une description en anglais alors que la consigne est en français, qu'est-ce qui se passe ? »

Le prompt système force "in French" deux fois explicitement. Si malgré ça le LLM dérive, le challenge est créé tel quel (on fait confiance au LLM avec un système de garde-fous structurel, pas linguistique). L'enseignant peut **toujours éditer** avant de cliquer Save — c'est un workflow human-in-the-loop, pas un workflow autonome.

### P5 — « Pourquoi tu ne stockes pas l'image en base mais juste l'URL ? »

3 raisons :
1. **Taille** : 200-500 KB par image × N challenges = base lourde et lente.
2. **CDN-friendly** : Pollinations sert l'image directement, pas besoin de proxy.
3. **Déterministe** : la seed garantit que la même URL donne toujours la même image — pas besoin de la "stocker", elle est reconstructible.

**Ce que je reconnais :** si Pollinations disparaît, mes URLs deviennent inutilisables. Mitigation : un job de cache local serait facile à ajouter.

---

## 5. Démo live — l'ordre à suivre

1. **Ouvre la liste publique** (`ChallengeList`) — montre les cards avec **image de couverture**, badge FEATURED, badge BOOST si actif.
2. **Clique « Rejoindre »** sur un challenge → snackbar de succès.
3. **Va dans « Mes Challenges »** → montre le **badge XP en haut** (niveau, progression).
4. **Soumets** un challenge.
5. **Switch vers le compte enseignant** → ouvre `ReviewSubmissions`.
6. **Valide** une soumission → certificat PDF généré.
7. **Ouvre le PDF** → montre le **fond blanc** + **QR code** en bas à droite.
8. **Va dans « Stats »** → clique **« Appliquer les décisions automatiques »** → montre le résultat (challenge featured, X challenges boostés).
9. **Retour dans `ManageChallenges`** → clique **« ✨ IA: Générer »** → tape un sujet → form auto-rempli.
10. **Save** → la card apparaît avec image générée par Pollinations.

**Ce que ce parcours démontre :** tu n'as pas juste cumulé des features, tu as un **flow utilisateur cohérent**.

---

## 6. Phrases à ne JAMAIS dire

- ❌ « C'est juste un projet d'école »
- ❌ « Je n'ai pas eu le temps »
- ❌ « C'est ChatGPT qui a écrit »
- ❌ « Je ne sais pas, c'est mon collègue qui a fait »
- ❌ « C'est facile »

## 7. Phrases à PLACER

- ✅ « Choix de design conscient parce que… »
- ✅ « Le trade-off ici était entre X et Y, j'ai choisi X parce que… »
- ✅ « En production, j'ajouterais Z pour scaler »
- ✅ « C'est une limite que j'ai identifiée, voici comment je l'attaquerais »
- ✅ « Bonne question — je n'ai pas de réponse claire, mon hypothèse est… »

---

## 8. Si tu paniques

Respire. Reformule la question pour gagner 5 secondes : *« Si je comprends bien, tu me demandes [X], c'est ça ? »*. Pendant qu'ils répondent, ton cerveau pioche la réponse.

**Si tu ne sais vraiment pas :** *« Honnêtement, je n'ai pas exploré ce point en profondeur. Mon intuition est [hypothèse]. Je vais le creuser après la soutenance. »*

C'est 100x mieux que d'inventer.

---

**Bon courage. Tu connais ton code mieux que le jury. Le seul piège possible, c'est toi qui te sous-estimes.**
