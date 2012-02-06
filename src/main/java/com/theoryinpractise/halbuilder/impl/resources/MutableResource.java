package com.theoryinpractise.halbuilder.impl.resources;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static com.theoryinpractise.halbuilder.ResourceFactory.WHITESPACE_SPLITTER;
import static java.lang.String.format;

public class MutableResource extends BaseResource implements Resource {

    public MutableResource(ResourceFactory resourceFactory, String href) {
        super(resourceFactory);
        this.links.add(new Link(resolveRelativeHref(resourceFactory.getBaseHref(), href), "self"));
    }

    public MutableResource(ResourceFactory resourceFactory) {
        super(resourceFactory);
    }

    /**
     * Add a link to this resource
     * @param href The target href for the link, relative to the href of this resource.
     * @param rel
     * @return
     */
    public MutableResource withLink(String href, String rel) {
        withLink(href, rel,
                Optional.of(Predicates.<ReadableResource>alwaysTrue()),
                Optional.<String>absent(),
                Optional.<String>absent(),
                Optional.<String>absent());
        return this;
    }

    /**
     * Add a link to this resource
     * @param href The target href for the link, relative to the href of this resource.
     * @param rel
     * @return
     */
    public MutableResource withLink(String href, String rel, Predicate<ReadableResource> predicate) {
        withLink(href, rel,
                Optional.of(predicate),
                Optional.<String>absent(),
                Optional.<String>absent(),
                Optional.<String>absent());
        return this;
    }

    /**
     * Add a link to this resource
     * @param href The target href for the link, relative to the href of this resource.
     * @param rel
     * @return
     */
    public MutableResource withLink(String href, String rel, Optional<Predicate<ReadableResource>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang) {
        if (predicate.or(Predicates.<ReadableResource>alwaysTrue()).apply(this)) {
            String resolvedHref = resolvableUri.matcher(href).matches() ? resolveRelativeHref(href) : href;
            for (String reltype : WHITESPACE_SPLITTER.split(rel)) {
                String resolvedRelType = resolvableUri.matcher(reltype).matches() ? resolveRelativeHref(reltype) : reltype;
                links.add(new Link(resolvedHref, resolvedRelType, name, title, hreflang));
            }
        }

        return this;
    }

    public Resource withProperty(String name, Object value) {
        if (properties.containsKey(name)) {
            throw new ResourceException(format("Duplicate property '%s' found for resource", name));
        }
        if (value != null) {
            properties.put(name, value);
        }
        return this;
    }

    public Resource withBean(Object value) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(value.getClass());
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (!"class".equals(pd.getName())) {
                    withProperty(pd.getName(), pd.getReadMethod().invoke(value));
                }
            }

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Resource withFields(Object value) {
        try {
            for (Field field : value.getClass().getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers())) {
                    withProperty(field.getName(), field.get(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public Resource withFieldBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, resourceFactory.newHalResource(href).withFields(o));
    }

    public Resource withBeanBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, resourceFactory.newHalResource(href).withBean(o));
    }

    /**
     * Adds a new namespace
     * @param namespace
     * @param href The target href of the namespace being added. This may be relative to the resourceFactories baseref
     * @return
     */
    public Resource withNamespace(String namespace, String href) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource", namespace));
        }
        namespaces.put(namespace, resolveRelativeHref(resourceFactory.getBaseHref(), href));
        return this;
    }

    public MutableResource withSubresource(String rel, Resource resource) {
        resource.withLink(resource.getSelfLink().getHref(), rel);
        resources.add(resource);
        return this;
    }

}