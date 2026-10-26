package com.consultorio.service.notificacion;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Motor Thymeleaf propio para los mails (no es un bean de Spring: así no reemplaza ni
 * activa la autoconfiguración de vistas MVC; la API sigue siendo solo JSON).
 * Plantillas en {@code classpath:templates/email/}: {@code <nombre>.html} (modo HTML, con
 * escape) y {@code <nombre>.txt} (modo TEXT), cada una con su layout común.
 */
@Component
public class PlantillasEmail {

  private static final String PREFIJO = "templates/email/";
  private static final Locale ES = Locale.forLanguageTag("es-UY");

  private final SpringTemplateEngine engine;

  public PlantillasEmail() {
    engine = new SpringTemplateEngine();
    engine.addTemplateResolver(resolver(TemplateMode.HTML, "*.html", 1));
    engine.addTemplateResolver(resolver(TemplateMode.TEXT, "*.txt", 2));
  }

  private static ClassLoaderTemplateResolver resolver(TemplateMode modo, String patron, int orden) {
    ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
    r.setPrefix(PREFIJO);
    r.setSuffix("");
    r.setTemplateMode(modo);
    r.setResolvablePatterns(java.util.Set.of(patron));
    r.setCharacterEncoding(StandardCharsets.UTF_8.name());
    r.setCacheable(true);
    r.setCheckExistence(true);
    r.setOrder(orden);
    return r;
  }

  /** Renderiza {@code <plantilla>.html} y {@code <plantilla>.txt} con las mismas variables. */
  public Renderizado renderizar(String plantilla, Map<String, Object> variables) {
    Context ctx = new Context(ES, variables);
    String html = engine.process(plantilla + ".html", ctx);
    String texto = engine.process(plantilla + ".txt", ctx);
    return new Renderizado(html, texto);
  }

  public record Renderizado(String html, String texto) {}
}
