package de.digitalcollections.iiif.bookshelf.frontend.controller;

import com.google.common.collect.Maps;
import de.digitalcollections.iiif.bookshelf.business.service.IiifManifestSummaryService;
import de.digitalcollections.iiif.bookshelf.frontend.model.PageWrapper;
import de.digitalcollections.iiif.bookshelf.model.IiifManifestSummary;
import de.digitalcollections.iiif.bookshelf.model.SearchRequest;
import de.digitalcollections.iiif.presentation.backend.api.exceptions.NotFoundException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {

  @Value("${authentication}")
  private boolean authentication;

  @Autowired
  private IiifManifestSummaryService iiifManifestSummaryService;

  @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
  public String list(Model model, Pageable pageRequest) {
    final Page<IiifManifestSummary> page = iiifManifestSummaryService.getAll(pageRequest);
    model.addAttribute("authentication", authentication);
    model.addAttribute("page", new PageWrapper(page, "/"));
    model.addAttribute("searchRequest", new SearchRequest());

    // model.addAttribute("manifests", iiifManifestSummaryService.getAll());
    // model.addAttribute("count", iiifManifestSummaryService.countAll());
    // model.addAttribute("infoUrl", "/iiif/image/" + identifier + "/info.json");
    return "index";
  }

  @RequestMapping(value = "/add", method = RequestMethod.GET)
  public String add(Model model) {
    model.addAttribute("manifest", new IiifManifestSummary());
    return "add";
  }

  @RequestMapping(value = "/add", method = RequestMethod.POST)
  public String add(IiifManifestSummary manifestSummary, Model model) {
    try {
      iiifManifestSummaryService.enrichAndSave(manifestSummary);
    } catch (ParseException e) {
      model.addAttribute("manifest", manifestSummary);
      model.addAttribute("errorMessage", "Manifest at URL contains malformed JSON.");
      return "add";
    } catch (NotFoundException e) {
      model.addAttribute("manifest", manifestSummary);
      model.addAttribute("errorMessage", "No Manifest was found at URL.");
      return "add";
    } catch (URISyntaxException e) {
      model.addAttribute("manifest", manifestSummary);
      model.addAttribute("errorMessage", "The value entered is not a valid URL.");
      return "add";
    }
    return "redirect:/";
  }

  @ResponseBody
  @RequestMapping(value = "/api/add", method = RequestMethod.POST, produces = "application/json")
  public IiifManifestSummary apiAdd(@RequestParam("uri") String manifestUri) throws ApiException {
    IiifManifestSummary summary = new IiifManifestSummary();
    summary.setManifestUri(manifestUri);
    try {
      iiifManifestSummaryService.enrichAndSave(summary);
      return summary;
    } catch (ParseException e) {
      throw new ApiException("Invalid JSON at URL.", HttpStatus.BAD_REQUEST);
    } catch (NotFoundException e) {
      throw new ApiException("No manifest at URL.", HttpStatus.BAD_REQUEST);
    } catch (URISyntaxException e) {
      throw new ApiException("Malformed URL.", HttpStatus.BAD_REQUEST);
    }
  }

  @RequestMapping(value = "/find", method = RequestMethod.GET)
  public String find(SearchRequest searchRequest, Model model, Pageable pageRequest) {
    final String term = searchRequest.getQuery().replace(":", "\\:");
    if (!StringUtils.isEmpty(term)) {
      final Page<IiifManifestSummary> page = iiifManifestSummaryService.findAll(term, pageRequest);
      model.addAttribute("authentication", authentication);
      model.addAttribute("page", new PageWrapper(page, "/"));
      model.addAttribute("searchRequest", searchRequest);
      return "index";
    }
    return "redirect:/";
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(value = {"/view/{uuid}"}, method = RequestMethod.GET)
  public String viewBook(@PathVariable UUID uuid, Model model) {
    IiifManifestSummary iiifManifestSummary = iiifManifestSummaryService.get(uuid);
    model.addAttribute("manifestId", iiifManifestSummary.getManifestUri());
    // model.addAttribute("canvasId", iiifPresentationEndpoint + identifier + "/canvas/p1");
    // return "bookreader/view-book";
    return "mirador/view";
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
    Map<String, Object> rv = Maps.newHashMap();
    rv.put("error", e.message);
    return ResponseEntity.status(e.statusCode).body(rv);
  }
}
