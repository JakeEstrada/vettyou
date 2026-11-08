package com.vettyou.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ReadmeParserTest {

	private final ReadmeParser parser = new ReadmeParser();

	private static final String README = """
			# Pantry **Tracker**

			[![build](https://img.shields.io/badge.svg)](https://ci)

			A web app that tracks what is in your kitchen
			so you stop buying doubles.

			## Contents
			- [Features](#features)

			## Features

			- Barcode scanning with the phone camera
			- [x] Expiry reminders by email
			- [ ] Shared household lists
			* Recipe ideas from what is left
			  - Sorted by what expires first

			### Accounts
			1. Sign up with **email** and password

			## Installation

			- Run `npm install`

			## Roadmap

			- Native mobile app

			```bash
			- not a bullet, inside code
			```
			""";

	@Test
	void readsTitleAndFirstParagraph() {
		assertThat(parser.title(README, "README.md")).isEqualTo("Pantry Tracker");
		assertThat(parser.summary(README))
			.isEqualTo("A web app that tracks what is in your kitchen so you stop buying doubles.");
	}

	@Test
	void keepsEveryBulletInOrderWithItsSection() {
		List<ReadmeParser.Feature> features = parser.features(README);

		assertThat(features).extracting(ReadmeParser.Feature::name)
			.containsExactly("Barcode scanning with the phone camera", "Expiry reminders by email",
					"Shared household lists", "Recipe ideas from what is left", "Sorted by what expires first",
					"Sign up with email and password", "Run npm install", "Native mobile app");
		Map<String, ReadmeParser.Feature> byName = features.stream()
			.collect(Collectors.toMap(ReadmeParser.Feature::name, Function.identity()));
		assertThat(byName.get("Barcode scanning with the phone camera").section()).isEqualTo("Features");
		assertThat(byName.get("Barcode scanning with the phone camera").lineNumber()).isEqualTo(13);
		assertThat(byName.get("Sign up with email and password").section()).isEqualTo("Accounts");
		assertThat(byName.get("Run npm install").section()).isEqualTo("Installation");
		assertThat(features).allSatisfy(feature -> assertThat(README).contains(feature.quote()));
	}

	@Test
	void marksUncheckedAndRoadmapBulletsAsPlanned() {
		Map<String, ReadmeParser.Feature> byName = parser.features(README)
			.stream()
			.collect(Collectors.toMap(ReadmeParser.Feature::name, Function.identity()));

		assertThat(byName.get("Shared household lists").planned()).isTrue();
		assertThat(byName.get("Native mobile app").planned()).isTrue();
		assertThat(byName.get("Expiry reminders by email").planned()).isFalse();
		assertThat(byName.get("Run npm install").planned()).isFalse();
	}

	@Test
	void usesFileNameWhenThereIsNoTitle() {
		String readme = """
				Notes app

				- Markdown editor with live preview
				- Offline sync
				""";

		assertThat(parser.features(readme)).extracting(ReadmeParser.Feature::section).containsOnly("README");
		assertThat(parser.title(readme, "notes-app.md")).isEqualTo("notes app");
	}

}
