/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rest;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.xwiki.rest.model.Attachment;
import org.xwiki.rest.model.Class;
import org.xwiki.rest.model.ClassProperty;
import org.xwiki.rest.model.Comment;
import org.xwiki.rest.model.HistorySummary;
import org.xwiki.rest.model.Link;
import org.xwiki.rest.model.ObjectSummary;
import org.xwiki.rest.model.Page;
import org.xwiki.rest.model.PageSummary;
import org.xwiki.rest.model.Properties;
import org.xwiki.rest.model.Relations;
import org.xwiki.rest.model.Space;
import org.xwiki.rest.model.Translations;
import org.xwiki.rest.model.XWikiRoot;
import org.xwiki.rest.resources.RootResource;
import org.xwiki.rest.resources.attachments.AttachmentAtPageVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentHistoryResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsAtPageVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.rest.resources.classes.ClassResource;
import org.xwiki.rest.resources.comments.CommentResource;
import org.xwiki.rest.resources.comments.CommentVersionResource;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.rest.resources.comments.CommentsVersionResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.pages.PageTranslationVersionResource;
import org.xwiki.rest.resources.pages.PageVersionResource;
import org.xwiki.rest.resources.pages.PagesResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 */
public class DomainObjectFactory
{
    public static XWikiRoot createXWikiRoot(Request request, com.xpn.xwiki.api.XWiki xwikiApi,
        XWikiResourceClassRegistry registry)
    {
        XWikiRoot xwikiRoot = new XWikiRoot(xwikiApi.getVersion());

        String fullUri =
            String.format("%s%s", request.getRootRef(), registry.getUriPatternForResourceClass(WikisResource.class));
        Link link = new Link(fullUri);
        link.setRel(Relations.WIKIS);
        xwikiRoot.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), registry.getUriPatternForResourceClass(RootResource.class));
        link = new Link(fullUri);
        link.setRel(Relations.WADL);
        link.setType(MediaType.APPLICATION_WADL_XML.toString());
        xwikiRoot.addLink(link);

        return xwikiRoot;
    }

    public static Space createSpace(Request request, XWikiResourceClassRegistry resourceClassRegistry, String wiki,
        String spaceName, String home, String homeXWikiUrl, int numberOfPages)
    {
        Space space = new Space(wiki, spaceName, home, homeXWikiUrl, numberOfPages);

        String fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PagesResource.class));
        Map<String, String> parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, wiki);
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
        Link link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGES);
        space.addLink(link);

        return space;
    }

    public static PageSummary createPageSummary(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc)
    {
        try {
            PageSummary pageSummary = new PageSummary();

            pageSummary.setWiki(doc.getWiki());
            pageSummary.setFullName(doc.getFullName());
            pageSummary.setId(doc.getPrefixedFullName());
            pageSummary.setSpace(doc.getSpace());
            pageSummary.setName(doc.getName());
            pageSummary.setTitle(doc.getTitle());
            pageSummary.setXWikiUrl(doc.getExternalURL("view"));

            Translations translations = pageSummary.getTranslations();

            List<String> languages = doc.getTranslationList();

            if (!languages.isEmpty()) {
                if (!doc.getDefaultLanguage().equals("")) {
                    translations.setDefaultTranslation(doc.getDefaultLanguage());
                }
            }

            String fullUri;
            Map<String, String> parametersMap;
            Link link;

            for (String language : languages) {
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageTranslationResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, language);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.TRANSLATION);
                link.setHrefLang(language);
                translations.addLink(link);
            }

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
            pageSummary.addLink(link);

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(SpaceResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.SPACE);
            pageSummary.addLink(link);

            String parent = doc.getParent();
            if (parent != null && parent.indexOf('.') != -1) {
                pageSummary.setParent(doc.getParent());

                String[] components = doc.getParent().split("\\.");
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, components[0]);
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, components[1]);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.PARENT);
                pageSummary.addLink(link);
            }

            return pageSummary;
        } catch (Exception e) {
            return null;
        }
    }

    public static Page createPage(Request request, XWikiResourceClassRegistry resourceClassRegistry, Document doc,
        boolean useVersion)
    {
        try {
            Page page = new Page();

            page.setWiki(doc.getWiki());
            page.setFullName(doc.getFullName());
            page.setId(doc.getPrefixedFullName());
            page.setSpace(doc.getSpace());
            page.setName(doc.getName());
            page.setTitle(doc.getTitle());
            page.setVersion(doc.getVersion());
            page.setMajorVersion(doc.getRCSVersion().at(0));
            page.setMinorVersion(doc.getRCSVersion().at(1));
            page.setLanguage(doc.getLanguage());
            page.setXWikiUrl(doc.getExternalURL("view"));
            page.setCreator(doc.getCreator());
            page.setCreated(doc.getCreationDate().getTime());
            page.setModifier(doc.getContentAuthor());
            page.setModified(doc.getContentUpdateDate().getTime());
            page.setContent(doc.getContent());

            Translations translations = page.getTranslations();

            List<String> languages = doc.getTranslationList();

            if (!languages.isEmpty()) {
                if (!doc.getDefaultLanguage().equals("")) {
                    translations.setDefaultTranslation(doc.getDefaultLanguage());
                }
            }

            String fullUri;
            Map<String, String> parametersMap;
            Link link;

            for (String language : languages) {
                if (useVersion) {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(PageTranslationVersionResource.class));
                } else {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(PageTranslationResource.class));
                }

                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", doc.getRCSVersion().at(0),
                    doc.getRCSVersion().at(1)));
                parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, language);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.TRANSLATION);
                link.setHrefLang(language);
                translations.addLink(link);
            }

            link = new Link(request.getResourceRef().getIdentifier());
            link.setRel(Relations.SELF);
            page.addLink(link);

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(SpaceResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.SPACE);
            page.addLink(link);

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageHistoryResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.HISTORY);
            page.addLink(link);

            String parent = doc.getParent();
            if (parent != null && parent.indexOf('.') != -1) {
                page.setParent(doc.getParent());

                String[] components = doc.getParent().split("\\.");

                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, components[0]);
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, components[1]);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.PARENT);
                page.addLink(link);
            }

            if (!doc.getComments().isEmpty()) {
                if (useVersion) {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(CommentsVersionResource.class));
                } else {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(CommentsResource.class));
                }
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", doc.getRCSVersion().at(0),
                    doc.getRCSVersion().at(1)));
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.COMMENTS);
                page.addLink(link);
            }

            if (!doc.getAttachmentList().isEmpty()) {
                if (useVersion) {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(AttachmentsAtPageVersionResource.class));
                } else {
                    fullUri =
                        String.format("%s%s", request.getRootRef(), resourceClassRegistry
                            .getUriPatternForResourceClass(AttachmentsResource.class));
                }
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", doc.getRCSVersion().at(0),
                    doc.getRCSVersion().at(1)));
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.ATTACHMENTS);
                page.addLink(link);
            }

            if (!doc.getxWikiObjects().keySet().isEmpty()) {
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(ObjectsResource.class));

                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.OBJECTS);
                page.addLink(link);
            }

            return page;
        } catch (Exception e) {
            return null;
        }
    }

    public static HistorySummary createHistorySummary(Request request,
        XWikiResourceClassRegistry resourceClassRegistry, String wikiName, String languageId, Object[] fields)
    {
        String pageId = (String) fields[0];

        String[] components = pageId.split("\\.");
        String spaceName = components[0];
        String pageName = components[1];

        XWikiRCSNodeId nodeId = (XWikiRCSNodeId) fields[1];
        int version = nodeId.getVersion().at(0);
        int minorVersion = nodeId.getVersion().at(1);

        Timestamp timestamp = (Timestamp) fields[2];
        String author = (String) fields[3];

        HistorySummary historySummary = new HistorySummary();
        historySummary.setPageId(pageId);
        historySummary.setModified(timestamp.getTime());
        historySummary.setModifier(author);
        historySummary.setVersion(version);
        historySummary.setMinorVersion(minorVersion);

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        if (languageId != null) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageTranslationVersionResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, wikiName);
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, pageName);
            parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, languageId);
            parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", version, minorVersion));
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageVersionResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, wikiName);
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, pageName);
            parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", version, minorVersion));
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
        }

        historySummary.addLink(link);

        return historySummary;
    }

    public static Comment createComment(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc, com.xpn.xwiki.api.Object xwikiComment, boolean useVersionInformation)
    {
        Comment comment = new Comment();
        comment.setId(xwikiComment.getNumber());

        Property property = xwikiComment.getProperty("author");
        if (property != null) {
            comment.setAuthor((String) property.getValue());
        }

        property = xwikiComment.getProperty("date");
        if (property != null) {
            comment.setDate(((Date) property.getValue()).getTime());
        }

        property = xwikiComment.getProperty("highlight");
        if (property != null) {
            comment.setHighlight((String) property.getValue());
        }

        property = xwikiComment.getProperty("comment");
        if (property != null) {
            comment.setText((String) property.getValue());
        }

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        if (useVersionInformation) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageVersionResource.class));
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageResource.class));
        }
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", doc.getRCSVersion().at(0), doc
            .getRCSVersion().at(1)));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        comment.addLink(link);

        if (useVersionInformation) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(CommentVersionResource.class));
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(CommentResource.class));
        }
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", doc.getRCSVersion().at(0), doc
            .getRCSVersion().at(1)));
        parametersMap.put(Constants.COMMENT_ID_PARAMETER, String.format("%d", xwikiComment.getNumber()));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.SELF);
        comment.addLink(link);

        return comment;
    }

    public static Attachment createAttachment(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc, com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiUrl, boolean useVersion)
    {
        Attachment attachment = new Attachment();

        attachment.setId(String.format("%s@%s", doc.getPrefixedFullName(), xwikiAttachment.getFilename()));
        attachment.setName(xwikiAttachment.getFilename());
        attachment.setSize(xwikiAttachment.getFilesize());
        attachment.setVersion(xwikiAttachment.getVersion());
        attachment.setPageVersion(xwikiAttachment.getDocument().getVersion());
        attachment.setMimeType(xwikiAttachment.getMimeType());
        attachment.setAuthor(xwikiAttachment.getAuthor());
        attachment.setDate(xwikiAttachment.getDate().getTime());
        attachment.setXWikiUrl(xwikiUrl);

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        if (useVersion) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageVersionResource.class));
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageResource.class));
        }

        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, xwikiAttachment.getDocument().getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, xwikiAttachment.getDocument().getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, xwikiAttachment.getDocument().getName());
        parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", xwikiAttachment.getDocument()
            .getRCSVersion().at(0), xwikiAttachment.getDocument().getRCSVersion().at(1)));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        attachment.addLink(link);

        if (useVersion) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(AttachmentAtPageVersionResource.class));
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(AttachmentResource.class));
        }
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, xwikiAttachment.getDocument().getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, xwikiAttachment.getDocument().getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, xwikiAttachment.getDocument().getName());
        parametersMap.put(Constants.PAGE_VERSION_PARAMETER, String.format("%d.%d", xwikiAttachment.getDocument()
            .getRCSVersion().at(0), xwikiAttachment.getDocument().getRCSVersion().at(1)));
        parametersMap.put(Constants.ATTACHMENT_NAME_PARAMETER, attachment.getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.ATTACHMENT_DATA);
        attachment.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(AttachmentHistoryResource.class));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.HISTORY);
        attachment.addLink(link);

        return attachment;
    }

    public static Attachment createAttachmentAtVersion(Request request,
        XWikiResourceClassRegistry resourceClassRegistry, com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiUrl)
    {
        Attachment attachment = new Attachment();

        attachment.setName(xwikiAttachment.getFilename());
        attachment.setSize(xwikiAttachment.getFilesize());
        attachment.setVersion(xwikiAttachment.getVersion());
        attachment.setPageVersion(xwikiAttachment.getDocument().getVersion());
        attachment.setMimeType(xwikiAttachment.getMimeType());
        attachment.setAuthor(xwikiAttachment.getAuthor());
        attachment.setDate(xwikiAttachment.getDate().getTime());
        attachment.setXWikiUrl(xwikiUrl);

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PageResource.class));

        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, xwikiAttachment.getDocument().getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, xwikiAttachment.getDocument().getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, xwikiAttachment.getDocument().getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        attachment.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(AttachmentVersionResource.class));

        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, xwikiAttachment.getDocument().getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, xwikiAttachment.getDocument().getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, xwikiAttachment.getDocument().getName());
        parametersMap.put(Constants.ATTACHMENT_NAME_PARAMETER, attachment.getName());
        parametersMap.put(Constants.ATTACHMENT_VERSION_PARAMETER, xwikiAttachment.getVersion());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.ATTACHMENT_DATA);
        attachment.addLink(link);

        return attachment;
    }

    public static ObjectSummary createObjectSummary(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc, com.xpn.xwiki.api.Object xwikiObject)
    {
        ObjectSummary objectSummary = new ObjectSummary();

        objectSummary.setId(String.format("%s:%s", doc.getPrefixedFullName(), xwikiObject.getGuid()));
        objectSummary.setGuid(xwikiObject.getGuid());
        objectSummary.setClassName(xwikiObject.getxWikiClass().getName());
        objectSummary.setNumber(xwikiObject.getNumber());
        objectSummary.setPageId(doc.getPrefixedFullName());
        objectSummary.setPrettyName(xwikiObject.getPrettyName());

        Properties properties = objectSummary.getPropertyList();

        for (Object propertyNameObject : xwikiObject.getPropertyNames()) {
            String propertyName = (String) propertyNameObject;
            Object propertyValue = xwikiObject.getProperty(propertyName).getValue();

            org.xwiki.rest.model.Property property = new org.xwiki.rest.model.Property();
            property.setName(propertyName);

            if (propertyValue != null) {
                property.setValue(propertyValue.toString());
            } else {
                property.setValue("");
            }

            properties.addProperty(property);
        }

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PageResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        objectSummary.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(ObjectResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        parametersMap.put(Constants.CLASS_NAME_PARAMETER, xwikiObject.getxWikiClass().getName());
        parametersMap.put(Constants.OBJECT_NUMBER_PARAMETER, String.format("%d", xwikiObject.getNumber()));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.SELF);
        objectSummary.addLink(link);

        return objectSummary;
    }

    private static com.xpn.xwiki.objects.classes.PropertyClass getPropertyType(XWikiContext xwikiContext,
        BaseObject object, String propertyName) throws XWikiException
    {
        BaseClass c = object.getxWikiClass(xwikiContext);

        for (Object o : c.getProperties()) {
            com.xpn.xwiki.objects.classes.PropertyClass propertyClass = (com.xpn.xwiki.objects.classes.PropertyClass) o;
            if (propertyClass.getName().equals(propertyName)) {
                return propertyClass;
            }
        }

        return null;
    }

    public static ObjectSummary createObjectSummary2(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        XWikiContext xwikiContext, Document doc, BaseObject xwikiObject) throws XWikiException
    {
        ObjectSummary objectSummary = new ObjectSummary();

        objectSummary.setId(String.format("%s:%s", doc.getPrefixedFullName(), xwikiObject.getGuid()));
        objectSummary.setGuid(xwikiObject.getGuid());
        objectSummary.setClassName(xwikiObject.getClassName());
        objectSummary.setNumber(xwikiObject.getNumber());
        objectSummary.setPageId(doc.getPrefixedFullName());
        objectSummary.setPrettyName(xwikiObject.getPrettyName());

        Properties properties = objectSummary.getPropertyList();

        for (String propertyName : xwikiObject.getPropertyNames()) {
            PropertyInterface xwikiProperty = xwikiObject.get(propertyName);
            PropertyClass xwikiPropertyType = getPropertyType(xwikiContext, xwikiObject, propertyName);

            org.xwiki.rest.model.Property property = new org.xwiki.rest.model.Property();

            if (xwikiPropertyType instanceof com.xpn.xwiki.objects.classes.ListClass) {
                com.xpn.xwiki.objects.classes.ListClass listProperty = (ListClass) xwikiPropertyType;

                Formatter f = new Formatter();
                List allowedValueList = listProperty.getList(xwikiContext);
                if (!allowedValueList.isEmpty()) {
                    for (int i = 0; i < allowedValueList.size(); i++) {
                        if (i != allowedValueList.size() - 1) {
                            f.format("%s,", allowedValueList.get(i).toString());
                        } else {
                            f.format("%s", allowedValueList.get(i).toString());
                        }
                    }

                    property.setAllowedValues(f.toString());
                }

                property.setSeparators(listProperty.getSeparators());
            }

            if (xwikiPropertyType instanceof com.xpn.xwiki.objects.classes.DateClass) {
                com.xpn.xwiki.objects.classes.DateClass dateProperty = (DateClass) xwikiPropertyType;

                property.setDateFormat(dateProperty.getDateFormat());
            }

            property.setType(xwikiPropertyType.getClassType());
            property.setName(propertyName);
            property.setValue(xwikiProperty.toFormString());
            properties.addProperty(property);
        }

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PageResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        objectSummary.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(ObjectResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        parametersMap.put(Constants.CLASS_NAME_PARAMETER, xwikiObject.getClassName());
        parametersMap.put(Constants.OBJECT_NUMBER_PARAMETER, String.format("%d", xwikiObject.getNumber()));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.SELF);
        objectSummary.addLink(link);

        return objectSummary;
    }

    public static Class createSpace(Request request, XWikiResourceClassRegistry resourceClassRegistry, String wiki,
        com.xpn.xwiki.api.Class xwikiClass)
    {
        org.xwiki.rest.model.Class theClass = new org.xwiki.rest.model.Class();
        theClass.setClassName(xwikiClass.getName());

        for (Object o : xwikiClass.getProperties()) {
            com.xpn.xwiki.api.PropertyClass xwikiClassProperty = (com.xpn.xwiki.api.PropertyClass) o;

            ClassProperty classProperty = new ClassProperty();
            classProperty.setName(xwikiClassProperty.getName());
            classProperty.setType(xwikiClassProperty.getClassType());

            for (Object xcp : xwikiClassProperty.getProperties()) {
                com.xpn.xwiki.api.Property xwikiProperty = (com.xpn.xwiki.api.Property) xcp;
                Object value = xwikiProperty.getValue();

                org.xwiki.rest.model.Property property = new org.xwiki.rest.model.Property();
                property.setName(xwikiProperty.getName());
                if (value != null) {
                    property.setValue(value.toString());
                } else {
                    property.setValue("");
                }
                classProperty.getPropertyList().getProperties().add(property);
            }

            theClass.getClassPropertyList().getClassProperties().add(classProperty);
        }

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(ClassResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, wiki);
        parametersMap.put(Constants.CLASS_NAME_PARAMETER, xwikiClass.getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.SELF);
        theClass.addLink(link);

        return theClass;
    }
}
