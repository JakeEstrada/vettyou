package com.vettyou.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vettyou.ai.OpenAiClient;

class MatchServiceFallbackTest {

	@Test
	void unreachableAiFallsBackToRulesSoAResumeCanStillBeBuilt() {
		Library library = MatchFixtures.jake();
		LibraryLoader loader = mock(LibraryLoader.class);
		when(loader.load(MatchFixtures.USER)).thenReturn(library);
		OpenAiClient ai = mock(OpenAiClient.class);
		when(ai.enabled()).thenReturn(true);
		when(ai.structured(anyString(), anyString(), anyString(), any()))
			.thenThrow(new OpenAiClient.AiException("The AI service could not be reached."));
		MatchService service = new MatchService(loader, MatchFixtures.DETECTOR, new EvidenceMatcher(ai, MatchFixtures.JSON));

		Judgments judged = service.judge(MatchFixtures.USER, "Staff engineer at Example",
				List.of(MatchFixtures.item("qualification", "required", "all", "skill", "Java", "Java", "java")));

		assertThat(judged.matcher()).isEqualTo("rules");
		assertThat(judged.parts()).isEmpty();
	}

}
