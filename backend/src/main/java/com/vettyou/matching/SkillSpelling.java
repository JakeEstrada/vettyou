package com.vettyou.matching;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Spellings of one product, such as "Node.js" and "NodeJS" or "PostgreSQL" and "Postgres". Related but different
 * things never count: ".NET" is not "ASP.NET Core", "Angular" is not "AngularJS", "Agile" is not "Scrum".
 */
final class SkillSpelling {

	private static final List<Set<String>> SAME = List.of(
			Set.of("postgresql", "postgres"),
			Set.of("react", "reactjs"),
			Set.of("vue", "vuejs"),
			Set.of("express", "expressjs"),
			Set.of("mongodb", "mongo"),
			Set.of("kubernetes", "k8s"),
			Set.of("go", "golang"),
			Set.of("googlecloud", "gcp"),
			Set.of("aws", "amazonwebservices"),
			Set.of("c#", "csharp"),
			Set.of("sqlserver", "mssql"),
			Set.of("sass", "scss"),
			Set.of("jwt", "jwts", "jsonwebtoken", "jsonwebtokens"),
			Set.of("websocket", "websockets"),
			Set.of("restapi", "restapis", "restfulapi", "restfulapis", "restservice", "restservices",
					"restfulservice", "restfulservices"),
			Set.of("tailwindcss", "tailwind"));

	private SkillSpelling() {
	}

	static boolean same(String name, String spelling) {
		String a = squash(name);
		String b = squash(spelling);
		return a.equals(b) || SAME.stream().anyMatch(set -> set.contains(a) && set.contains(b));
	}

	/** The posting's spelling, unless it only differs in letter case. */
	static String shown(String name, String spelling) {
		return spelling.equalsIgnoreCase(name) ? name : spelling;
	}

	private static String squash(String text) {
		return text.toLowerCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
	}

}
