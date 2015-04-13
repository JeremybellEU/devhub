package nl.tudelft.ewi.devhub.server.web.templating;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateEngine {

	private final Configuration conf;
	private final TranslatorFactory translatorFactory;

	@Inject
	@SneakyThrows
	public TemplateEngine(@Named("directory.templates") final File templatesDirectory, TranslatorFactory translatorFactory) {
		this.translatorFactory = translatorFactory;
		this.conf = new Configuration(Configuration.VERSION_2_3_0) {
			{
				setDirectoryForTemplateLoading(templatesDirectory);
				setDefaultEncoding(Charsets.UTF_8.displayName());
				setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
				setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
				setTemplateLoader(new FileTemplateLoader(templatesDirectory) {
					public Reader getReader(Object templateSource, String encoding) throws IOException {
						return new WrappedReader(super.getReader(templateSource, encoding), "[#escape x as x?html]", "[/#escape]");
					};
				});
			}
		};
	}

	public String process(String template, List<Locale> locales) throws IOException {
		return process(template, locales, null);
	}

	public String process(String template, List<Locale> locales, Map<String, ?> parameters) throws IOException {
		Translator translator = translatorFactory.create(locales);
		
		try {
			Builder<String, Object> builder = ImmutableMap.<String, Object> builder();
			builder.put("i18n", translator);
			if (parameters != null) {
				builder.putAll(parameters);
			}
			
			StringWriter out = new StringWriter();
			conf.getTemplate(template).process(builder.build(), out);
			return out.toString();
		}
		catch (TemplateException e) {
			throw new IOException(e);
		}
	}

}