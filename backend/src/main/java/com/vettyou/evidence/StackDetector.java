package com.vettyou.evidence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Finds technologies a README names, with the exact line each came from. The README is untrusted text: it is only
 * matched against this fixed list, never interpreted.
 */
@Component
public class StackDetector {

	public record Detection(String name, String category, int lineNumber, String quote, boolean planned) {
	}

	/** A listed technology named in a text: its listed name, the words used, and where. */
	public record Mention(String name, String text, int start, int end) {
	}

	private record Rule(String name, String category, Pattern pattern) {
	}

	private static final int QUOTE_LIMIT = 300;

	private static final Pattern PLANNED = Pattern.compile(
			"(?i)\\b(plan|plans|planned|planning|todo|to-do|roadmap|coming soon|future|will add|not yet|wip|work in progress)\\b");

	private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s");

	private static final List<Rule> RULES = List.of(
			rule("Java", "language", "Java"),
			rule("JavaScript", "language", "(?i:JavaScript)"),
			rule("TypeScript", "language", "(?i:TypeScript)"),
			rule("Python", "language", "(?i:Python)"),
			rule("C#", "language", "C#|(?i:csharp)"),
			rule("C++", "language", "C\\+\\+"),
			rule("Go", "language", "(?i:Golang)"),
			rule("Rust", "language", "Rust"),
			rule("Kotlin", "language", "(?i:Kotlin)"),
			rule("Swift", "language", "Swift"),
			rule("Ruby", "language", "Ruby"),
			rule("PHP", "language", "(?i:PHP)"),
			rule("Dart", "language", "Dart"),
			rule("SQL", "language", "SQL"),
			rule("HTML", "language", "(?i:HTML5?)"),
			rule("CSS", "language", "(?i:CSS3?)"),
			rule("Sass", "language", "(?i:SCSS|Sass)"),
			rule("Bash", "language", "(?i:Bash)"),
			rule("Spring Boot", "framework", "(?i:Spring\\s?Boot)"),
			rule("Spring Security", "framework", "(?i:Spring Security)"),
			rule("Angular", "framework", "(?i:Angular(?:JS)?)"),
			rule("React", "framework", "React(?:\\.js|JS)?"),
			rule("React Native", "framework", "(?i:React Native)"),
			rule("Vue", "framework", "(?i:Vue(?:\\.js)?)"),
			rule("Next.js", "framework", "(?i:Next\\.js|NextJS)"),
			rule("Nuxt", "framework", "(?i:Nuxt(?:\\.js)?)"),
			rule("Svelte", "framework", "(?i:Svelte(?:Kit)?)"),
			rule("Express", "framework", "(?i:Express\\.js|ExpressJS)|Express(?= API| server| backend| app)"),
			rule("NestJS", "framework", "(?i:Nest\\.?JS)"),
			rule("Node.js", "framework", "(?i:Node\\.?js)"),
			rule("Django", "framework", "(?i:Django)"),
			rule("Flask", "framework", "Flask"),
			rule("FastAPI", "framework", "(?i:FastAPI)"),
			rule("Ruby on Rails", "framework", "(?i:Ruby on Rails)|Rails"),
			rule("Laravel", "framework", "(?i:Laravel)"),
			rule(".NET", "framework", "ASP\\.NET(?: Core)?|\\.NET(?: Core)?|(?i:dotnet)"),
			rule("Tailwind CSS", "framework", "(?i:Tailwind(?:\\s?CSS)?)"),
			rule("Bootstrap", "framework", "Bootstrap"),
			rule("jQuery", "framework", "(?i:jQuery)"),
			rule("Redux", "framework", "(?i:Redux)"),
			rule("Flutter", "framework", "(?i:Flutter)"),
			rule("Electron", "framework", "Electron"),
			rule("Socket.IO", "framework", "(?i:Socket\\.IO)"),
			rule("Hibernate", "framework", "Hibernate"),
			rule("TensorFlow", "framework", "(?i:TensorFlow)"),
			rule("PyTorch", "framework", "(?i:PyTorch)"),
			rule("pandas", "framework", "pandas|Pandas"),
			rule("NumPy", "framework", "(?i:NumPy)"),
			rule("PostgreSQL", "database", "(?i:PostgreSQL|Postgres)"),
			rule("MySQL", "database", "(?i:MySQL)"),
			rule("SQLite", "database", "(?i:SQLite)"),
			rule("MongoDB", "database", "(?i:MongoDB|Mongo)"),
			rule("Redis", "database", "(?i:Redis)"),
			rule("DynamoDB", "database", "(?i:DynamoDB)"),
			rule("Elasticsearch", "database", "(?i:Elasticsearch)"),
			rule("Firebase", "database", "(?i:Firebase|Firestore)"),
			rule("Supabase", "database", "(?i:Supabase)"),
			rule("SQL Server", "database", "(?i:SQL Server|MSSQL)"),
			rule("Neo4j", "database", "(?i:Neo4j)"),
			rule("Docker", "platform", "(?i:Docker)"),
			rule("Kubernetes", "platform", "(?i:Kubernetes|K8s)"),
			rule("AWS", "platform", "AWS|(?i:Amazon Web Services)"),
			rule("Azure", "platform", "Azure"),
			rule("Google Cloud", "platform", "(?i:Google Cloud|GCP)"),
			rule("Vercel", "platform", "(?i:Vercel)"),
			rule("Netlify", "platform", "(?i:Netlify)"),
			rule("Heroku", "platform", "(?i:Heroku)"),
			rule("Linux", "platform", "(?i:Linux)"),
			rule("Nginx", "platform", "(?i:Nginx)"),
			rule("GitHub Actions", "platform", "(?i:GitHub Actions)"),
			rule("Terraform", "platform", "(?i:Terraform)"),
			rule("Kafka", "platform", "(?i:Kafka)"),
			rule("RabbitMQ", "platform", "(?i:RabbitMQ)"),
			rule("GraphQL", "platform", "(?i:GraphQL)"),
			rule("Stripe", "platform", "Stripe"),
			rule("OpenAI API", "platform", "(?i:OpenAI)"),
			rule("Maven", "tool", "Maven"),
			rule("Gradle", "tool", "(?i:Gradle)"),
			rule("Flyway", "tool", "(?i:Flyway)"),
			rule("Vite", "tool", "Vite"),
			rule("Webpack", "tool", "(?i:Webpack)"),
			rule("Jest", "tool", "Jest"),
			rule("JUnit", "tool", "(?i:JUnit)"),
			rule("Vitest", "tool", "(?i:Vitest)"),
			rule("Playwright", "tool", "(?i:Playwright)"),
			rule("Cypress", "tool", "Cypress"),
			rule("Selenium", "tool", "(?i:Selenium)"),
			rule("Testcontainers", "tool", "(?i:Testcontainers)"),
			rule("Git", "tool", "Git"),
			rule("Prisma", "tool", "(?i:Prisma)"),
			rule("Mongoose", "tool", "(?i:Mongoose)"),
			rule("WebSockets", "tool", "(?i:WebSockets?)"),
			rule("Entity Framework", "framework", "(?i:Entity Framework(?: Core)?|EF Core)"),
			rule("Avalonia", "framework", "(?i:Avalonia(?: UI)?)"),
			rule("Fastify", "framework", "(?i:Fastify)"),
			rule("Three.js", "framework", "(?i:Three\\.js|ThreeJS)"),
			rule("TanStack Query", "framework", "(?i:TanStack Query|React Query)"),
			rule("Drizzle", "tool", "(?i:Drizzle(?: ORM)?)"),
			rule("Zod", "tool", "Zod"),
			rule("JWT", "tool", "JWTs?|(?i:JSON Web Tokens?)"),
			rule("REST APIs", "platform", "RESTful(?: APIs?| services?)?|REST(?: APIs?| services?)?"),
			rule("Twilio", "platform", "(?i:Twilio)"),
			rule("Plaid", "platform", "Plaid"),
			rule("Microsoft Graph", "platform", "(?i:Microsoft Graph)"),
			rule("Raspberry Pi", "platform", "(?i:Raspberry Pi)"),
			rule("Arduino", "platform", "(?i:Arduino)"),
			rule("PowerShell", "language", "(?i:PowerShell)"),
			rule("CI/CD", "practice", "CI/CD|(?i:continuous integration)"),
			rule("Agile", "practice", "Agile|(?i:Scrum)"),
			rule("Jira", "tool", "(?i:Jira)"),
			rule("Figma", "tool", "(?i:Figma)"),
			rule("Excel", "tool", "(?:Microsoft |MS )?Excel(?! at\\b| in\\b)"),
			rule("Power BI", "tool", "(?i:Power ?BI)"),
			rule("Tableau", "tool", "(?i:Tableau)"));

	private static Rule rule(String name, String category, String regex) {
		return new Rule(name, category, Pattern.compile("(?<![A-Za-z0-9_])(?:" + regex + ")(?![A-Za-z0-9_])"));
	}

	/** The listed name when the text names exactly one listed technology, otherwise the text unchanged. */
	public String canonical(String text) {
		List<Detection> found = detect(text);
		return found.size() == 1 ? found.get(0).name() : text;
	}

	/** The pattern for a listed name, so "Postgres" in a write-up counts for "PostgreSQL". */
	public Optional<Pattern> pattern(String name) {
		return RULES.stream().filter(rule -> rule.name().equalsIgnoreCase(name)).map(Rule::pattern).findFirst();
	}

	public Optional<String> category(String name) {
		return RULES.stream().filter(rule -> rule.name().equalsIgnoreCase(name)).map(Rule::category).findFirst();
	}

	/**
	 * Every place the text names a listed technology, in order. A name inside a longer one ("SQL" in "SQL Server",
	 * "React" in "React Native") is not counted.
	 */
	public List<Mention> mentions(String text) {
		List<Mention> all = new ArrayList<>();
		for (Rule rule : RULES) {
			Matcher matcher = rule.pattern().matcher(text);
			while (matcher.find()) {
				all.add(new Mention(rule.name(), matcher.group(), matcher.start(), matcher.end()));
			}
		}
		return all.stream()
			.filter(mention -> all.stream()
				.noneMatch(other -> other.start() <= mention.start() && other.end() >= mention.end()
						&& other.end() - other.start() > mention.end() - mention.start()))
			.sorted(Comparator.comparingInt(Mention::start))
			.toList();
	}

	public List<Detection> detect(String body) {
		Map<String, Detection> found = new LinkedHashMap<>();
		String[] lines = body.split("\\R", -1);
		boolean plannedSection = false;
		for (int index = 0; index < lines.length; index++) {
			String line = lines[index];
			if (HEADING.matcher(line).find()) {
				plannedSection = PLANNED.matcher(line).find();
			}
			boolean planned = plannedSection || PLANNED.matcher(line).find();
			for (Rule rule : RULES) {
				if (found.containsKey(rule.name())) {
					continue;
				}
				Matcher matcher = rule.pattern().matcher(line);
				if (matcher.find()) {
					found.put(rule.name(),
							new Detection(rule.name(), rule.category(), index + 1, quote(line, matcher.start()), planned));
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	private static String quote(String line, int matchStart) {
		String trimmed = line.strip();
		int offset = line.indexOf(trimmed);
		if (trimmed.length() <= QUOTE_LIMIT) {
			return trimmed;
		}
		int start = Math.max(0, matchStart - offset - QUOTE_LIMIT / 3);
		int end = Math.min(trimmed.length(), start + QUOTE_LIMIT);
		return trimmed.substring(start, end);
	}

}
