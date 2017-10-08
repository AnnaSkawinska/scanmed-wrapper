package software.xsolve.springcloud.scanmed.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import software.xsolve.springcloud.scanmed.resource.DoctorSlot;
import software.xsolve.springcloud.scanmed.resource.VisitResponse;
import software.xsolve.springcloud.scanmed.service.LocalDateTimeConverter;

@RestController //doWyczajenia
public class ScanmedController {

	@Autowired
	LocalDateTimeConverter localDateTimeConverter;

	@RequestMapping("/scanmed")
	public VisitResponse fetchScanmedResponse(
			@RequestParam(value="location", defaultValue = "") String location,
			@RequestParam(value="specialty") String specialty) throws IOException, InterruptedException {

		try (final WebClient webClient = new WebClient()) {
			configureWebClient(webClient);

			final HtmlPage page = webClient.getPage(
					"https://www.e-scanmed.pl/ron-www/placowkaMedyczna/znajdz?searchInput=" + specialty + " " + location);

			webClient.waitForBackgroundJavaScript(10000);

			return VisitResponse.builder()
					.doctorSlots(parseResults(page))
					.build();
		}
		catch (FailingHttpStatusCodeException exception) {
			// one of page elements being fetched with ajax is not available - we don't mind
		}

		return VisitResponse.builder().build();
	}

	private List<DoctorSlot>  parseResults(HtmlPage page) {
		List<DoctorSlot> slots = new ArrayList<>();
		DomNodeList<DomNode> resultNodes = page.querySelectorAll(".card-item");

		for (DomNode resultNode : resultNodes) {
			parseSlot(resultNode).ifPresent(slots::add);
		}
		return slots;
	}

	private Optional<DoctorSlot> parseSlot(DomNode resultNode) {
		DomNode nameNode = resultNode.querySelector(".c-i-name");
		DomNode dateNode = resultNode.querySelector(".c-i-date");
		DomNode timeNode = resultNode.querySelector(".c-i-time");

		if (dateNode != null && timeNode != null) {
			return Optional.of(DoctorSlot.builder()
					.name(nameNode.asText())
					.timeSlot(localDateTimeConverter.convert(dateNode.asText(), timeNode.asText())).build());
		} else {
			return Optional.empty();
		}
	}

	private void configureWebClient(WebClient webClient) {
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setCssEnabled(false);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
	}
}
