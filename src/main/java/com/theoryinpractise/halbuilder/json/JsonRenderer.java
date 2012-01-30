package com.theoryinpractise.halbuilder.json;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.Renderer;
import com.theoryinpractise.halbuilder.Resource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsonRenderer<T> implements Renderer<T> {

    public Optional<T> render(ReadableResource resource, Writer writer) {

        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(writer);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            g.writeStartObject();
            renderJson(g, resource, false);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.absent();
    }

    private void renderJson(JsonGenerator g, ReadableResource resource, boolean embedded) throws IOException {

        final Link selfLink = resource.getSelfLink();
        final String href = selfLink.getHref();

        if (!resource.getCanonicalLinks().isEmpty() || (!embedded && !resource.getNamespaces().isEmpty())) {
            g.writeObjectFieldStart(LINKS);

            List<Link> links = Lists.newArrayList();

            // Include namespaces as links when not embedded
            if (!embedded) {
                for (Map.Entry<String, String> entry : resource.getNamespaces().entrySet()) {
                    links.add(new Link(entry.getValue(), CURIE, Optional.of(entry.getKey()), Optional.<String>absent(), Optional.<String>absent()));
                }
            }

            // Add resource links
            links.addAll(resource.getLinks());

            // Partition resource links by rel
            Multimap<String, Link> linkMap = Multimaps.index(links, new Function<Link, String>() {
                public String apply(@Nullable Link link) {
                    return link.getRel();
                }
            });

            for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {
                if (linkEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(linkEntry.getKey());
                    g.writeStringField(HREF, linkEntry.getValue().iterator().next().getHref());
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(linkEntry.getKey());
                    for (Link link : linkEntry.getValue()) {
                        g.writeStartObject();
                        g.writeStringField(HREF, link.getHref());
                        if (link.getName().isPresent()) {
                            g.writeStringField(NAME, link.getName().get());
                        }
                        if (link.getTitle().isPresent()) {
                            g.writeStringField(TITLE, link.getTitle().get());
                        }
                        if (link.getHreflang().isPresent()) {
                            g.writeStringField(HREFLANG, link.getHreflang().get());
                        }
                        g.writeEndObject();
                    }
                    g.writeEndArray();
                }
            }
            g.writeEndObject();
        }

        for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {
            g.writeObjectField(entry.getKey(), entry.getValue());
        }

        if (!resource.getResources().isEmpty()) {
            g.writeObjectFieldStart(EMBEDDED);

            Multimap<String, Resource> resourceMap = Multimaps.index(resource.getResources(), new Function<Resource, String>() {
                public String apply(@Nullable Resource resource) {
                    List<String> relTypes = Lists.newArrayList(Splitter.on(" ").split(resource.getRel()));

                    Iterables.removeIf(relTypes, new Predicate<String>() {
                        public boolean apply(@Nullable String s) {
                            return "self".equals(s);
                        }
                    });

                    return Joiner.on(" ").join(relTypes);
                }
            });

            for (Map.Entry<String, Collection<Resource>> resourceEntry : resourceMap.asMap().entrySet()) {
                if (resourceEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(resourceEntry.getKey());
                    ReadableResource subResource = resourceEntry.getValue().iterator().next();
                    renderJson(g, subResource, true);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (ReadableResource halResource : resourceEntry.getValue()) {
                        g.writeStartObject();
                        ReadableResource subResource = resourceEntry.getValue().iterator().next();
                        renderJson(g, subResource, true);
                        g.writeEndObject();
                    }
                }
            }
        }
    }
}
