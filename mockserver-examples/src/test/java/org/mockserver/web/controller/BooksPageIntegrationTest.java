package org.mockserver.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.proxy.Times;
import org.mockserver.configuration.RootConfiguration;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Book;
import org.mockserver.model.Parameter;
import org.mockserver.web.configuration.WebMvcConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import java.util.Arrays;

import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author jamesdbloom
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration(
                classes = {
                        RootConfiguration.class
                }
        ),
        @ContextConfiguration(
                classes = {
                        WebMvcConfiguration.class
                }
        )
})
public class BooksPageIntegrationTest {

    private ClientAndProxy proxy;
    private ClientAndServer mockServer;
    @Resource
    private Environment environment;
    @Resource
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void setupFixture() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Before
    public void startProxy() {
        mockServer = startClientAndServer(environment.getProperty("bookService.port", Integer.class));
        proxy = startClientAndProxy(environment.getProperty("bookService.proxyPort", Integer.class));
    }

    @Test
    public void shouldLoadListOfBooks() throws Exception {
        // given
        mockServer
                .when(
                        request()
                                .withPath("/get_books")
                )
                .respond(
                        response()
                                .withBody("" +
                                        "[\n" +
                                        "    {\n" +
                                        "        \"id\": \"1\",\n" +
                                        "        \"title\": \"Xenophon's imperial fiction : on the education of Cyrus\",\n" +
                                        "        \"author\": \"James Tatum\",\n" +
                                        "        \"isbn\": \"0691067570\",\n" +
                                        "        \"publicationDate\": \"1989\"\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "        \"id\": \"2\",\n" +
                                        "        \"title\": \"You are here : personal geographies and other maps of the imagination\",\n" +
                                        "        \"author\": \"Katharine A. Harmon\",\n" +
                                        "        \"isbn\": \"1568984308\",\n" +
                                        "        \"publicationDate\": \"2004\"\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "        \"id\": \"3\",\n" +
                                        "        \"title\": \"You just don't understand : women and men in conversation\",\n" +
                                        "        \"author\": \"Deborah Tannen\",\n" +
                                        "        \"isbn\": \"0345372050\",\n" +
                                        "        \"publicationDate\": \"1990\"\n" +
                                        "    }" +
                                        "]")
                );

        // when
        MvcResult response = mockMvc.perform(get("/books").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn();

        // then
        BooksPage booksPage = new BooksPage(response);
        booksPage.containsListOfBooks(Arrays.asList(
                new Book(1, "Xenophon's imperial fiction : on the education of Cyrus", "James Tatum", "0691067570", "1989"),
                new Book(2, "You are here : personal geographies and other maps of the imagination", "Katharine A. Harmon", "1568984308", "2004"),
                new Book(3, "You just don't understand : women and men in conversation", "Deborah Tannen", "0345372050", "1990")
        ));
        proxy.verify(
                request()
                        .withPath("/get_books"),
                Times.exactly(1)
        );
    }

    @Test
    public void shouldLoadSingleBook() throws Exception {
        // given
        mockServer
                .when(
                        request()
                                .withPath("/get_book")
                                .withParameters(
                                        new Parameter("id", "1")
                                )
                )
                .respond(
                        response()
                                .withBody("" +
                                        "{\n" +
                                        "    \"id\": \"1\",\n" +
                                        "    \"title\": \"Xenophon's imperial fiction : on the education of Cyrus\",\n" +
                                        "    \"author\": \"James Tatum\",\n" +
                                        "    \"isbn\": \"0691067570\",\n" +
                                        "    \"publicationDate\": \"1989\"\n" +
                                        "}")
                );

        MvcResult response = mockMvc.perform(get("/book/1").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn();

        BookPage bookPage = new BookPage(response);
        bookPage.containsBook(new Book(1, "Xenophon's imperial fiction : on the education of Cyrus", "James Tatum", "0691067570", "1989"));
        proxy.verify(
                request()
                        .withPath("/get_book")
                        .withParameters(
                                new Parameter("id", "1")
                        ),
                Times.exactly(1)
        );
    }

    @After
    public void stopProxy() {
        proxy.stop();
        mockServer.stop();
    }

}
