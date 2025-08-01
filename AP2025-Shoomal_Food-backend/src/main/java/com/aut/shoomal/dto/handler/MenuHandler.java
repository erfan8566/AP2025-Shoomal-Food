package com.aut.shoomal.dto.handler;

import com.aut.shoomal.entity.menu.Menu;
import com.aut.shoomal.entity.user.User;
import com.aut.shoomal.entity.user.UserManager;
import com.aut.shoomal.entity.user.Seller;
import com.aut.shoomal.entity.food.FoodManager;
import com.aut.shoomal.entity.menu.MenuManager;
import com.aut.shoomal.entity.restaurant.RestaurantManager;
import com.aut.shoomal.dao.BlacklistedTokenDao;
import com.aut.shoomal.dto.request.AddMenuItemRequest;
import com.aut.shoomal.dto.request.AddMenuTitleRequest;
import com.aut.shoomal.dto.response.ApiResponse;
import com.aut.shoomal.dto.response.MenuTitleResponse;
import com.aut.shoomal.exceptions.*;
import com.aut.shoomal.util.HibernateUtil;
import com.sun.net.httpserver.HttpExchange;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuHandler extends AbstractHttpHandler {

    private final RestaurantManager restaurantManager;
    private final MenuManager menuManager;
    private final FoodManager foodManager;
    protected final UserManager userManager;
    protected final BlacklistedTokenDao blacklistedTokenDao;

    private static final Pattern MENU_PATH_PATTERN = Pattern.compile("/restaurants/(\\d+)/menu/([^/]+)(?:/(\\d+))?.*");
    private static final Pattern RESTAURANT_ID_FROM_MENU_PATH_PATTERN = Pattern.compile("/restaurants/(\\d+)/menu.*");
    private static final Pattern MENU_TITLE_FROM_PATH_PATTERN = Pattern.compile("/restaurants/\\d+/menu/([^/]+).*");
    private static final Pattern MENU_ITEM_ID_FROM_PATH_PATTERN = Pattern.compile("/restaurants/\\d+/menu/[^/]+/(\\d+).*");
    private static final Pattern EDIT_MENU_PATH_PATTERN = Pattern.compile("/restaurants/\\d+/menu/edit/([^/]+).*");

    public MenuHandler(RestaurantManager restaurantManager, MenuManager menuManager, FoodManager foodManager, UserManager userManager, BlacklistedTokenDao blacklistedTokenDao) {
        this.restaurantManager = restaurantManager;
        this.menuManager = menuManager;
        this.foodManager = foodManager;
        this.userManager = userManager;
        this.blacklistedTokenDao = blacklistedTokenDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        User authenticatedUser = authenticate(exchange, userManager, blacklistedTokenDao);
        if (authenticatedUser == null) {
            return;
        }

        try {
            Optional<Integer> restaurantIdOptional = extractIdFromPath(requestPath, RESTAURANT_ID_FROM_MENU_PATH_PATTERN);
            int restaurantId = restaurantIdOptional.orElse(-1);

            Optional<String> menuTitleOptional = extractMenuTitleFromPath(requestPath, MENU_TITLE_FROM_PATH_PATTERN);
            String menuTitle = menuTitleOptional.orElse(null);

            Optional<String> newMenu = extractMenuTitleFromPath(requestPath, EDIT_MENU_PATH_PATTERN);
            String newMenuTitle = newMenu.orElse(null);

            Optional<Integer> menuItemIdOptional = extractIdFromPath(requestPath, MENU_ITEM_ID_FROM_PATH_PATTERN);
            int menuItemId = menuItemIdOptional.orElse(-1);

            if (requestPath.equals("/restaurants/" + restaurantId + "/menu") && method.equalsIgnoreCase("POST") && restaurantId != -1) {
                handleAddMenuTitle(exchange, authenticatedUser, restaurantId);
            } else if (EDIT_MENU_PATH_PATTERN.matcher(requestPath).matches() && method.equalsIgnoreCase("PUT") && restaurantId != -1 && newMenuTitle != null) {
                editMenuTitle(exchange, restaurantId, newMenuTitle);
            } else if (MENU_ITEM_ID_FROM_PATH_PATTERN.matcher(requestPath).matches() && method.equalsIgnoreCase("DELETE") && restaurantId != -1 && menuTitle != null && menuItemId != -1) {
                handleDeleteMenuItemFromMenu(exchange, authenticatedUser, restaurantId, menuTitle, menuItemId);
            } else if (MENU_TITLE_FROM_PATH_PATTERN.matcher(requestPath).matches() && restaurantId != -1 && menuTitle != null) {
                if (method.equalsIgnoreCase("DELETE")) {
                    handleDeleteMenuTitle(exchange, authenticatedUser, restaurantId, menuTitle);
                } else if (method.equalsIgnoreCase("PUT")) {
                    handleAddMenuItem(exchange, authenticatedUser, restaurantId, menuTitle);
                } else if (method.equalsIgnoreCase("GET")) {
                    getMenuByTitle(exchange, restaurantId, menuTitle);
                } else {
                    sendResponse(exchange, HttpURLConnection.HTTP_BAD_METHOD, new ApiResponse(false, "Method Not Allowed. Expected DELETE or PUT"));
                }
            } else if (method.equalsIgnoreCase("GET") && requestPath.equals("/restaurants/" + restaurantId + "/menus") && restaurantId != -1) {
                getMenusByRestaurantId(exchange, (long) restaurantId);
            } else {
                sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, new ApiResponse(false, "Resource not found"));
            }
        } catch (NotFoundException e) {
            sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, new ApiResponse(false, "Resource not found: " + e.getMessage()));
        } catch (InvalidInputException e) {
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "Invalid input: " + e.getMessage()));
        } catch (ForbiddenException e) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: " + e.getMessage()));
        } catch (ConflictException e) {
            sendResponse(exchange, HttpURLConnection.HTTP_CONFLICT, new ApiResponse(false, "Conflict: " + e.getMessage()));
        } catch (ServiceUnavailableException e) {
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, new ApiResponse(false, "Service temporarily unavailable: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, new ApiResponse(false, "Internal server error: " + e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void getMenuByTitle(HttpExchange exchange, int restaurantId, String menuTitle) throws IOException
    {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Menu menu = menuManager.findByTitle(session, restaurantId, menuTitle);
        if (menu == null)
        {
            session.close();
            throw new NotFoundException("Menu not found");
        }
        sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, new MenuTitleResponse(menu.getTitle()));
        session.close();
    }

    private void getMenusByRestaurantId(HttpExchange exchange, Long restaurantId) throws IOException
    {
        List<Menu> menus = menuManager.findMenusByRestaurantId(restaurantId);
        if (menus == null)
            throw new NotFoundException("No menu found for restaurant id " + restaurantId);
        List<MenuTitleResponse> responses = menus.stream()
                .map(menu -> new MenuTitleResponse(menu.getTitle()))
                .toList();
        sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, responses);
    }

    private void handleAddMenuTitle(HttpExchange exchange, User authenticatedUser, Integer restaurantId) throws IOException {
        if (!checkHttpMethod(exchange, "POST")) return;
        if (!restaurantManager.isOwner(restaurantId, String.valueOf(authenticatedUser.getId()))) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Not the owner of this restaurant."));
            return;
        }
        if (!(authenticatedUser instanceof Seller) || !((Seller) authenticatedUser).isApproved()) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Seller is not yet approved to manage menus."));
            return;
        }
        AddMenuTitleRequest request = parseRequestBody(exchange, AddMenuTitleRequest.class);
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "Invalid input: 'title' is required."));
            return;
        }
        menuManager.addMenuTitle(restaurantId, request.getTitle(), String.valueOf(authenticatedUser.getId()));
        sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, new MenuTitleResponse(request.getTitle()));
    }

    private void editMenuTitle(HttpExchange exchange, Integer restaurantId, String title) throws IOException
    {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        Menu menu = menuManager.findByTitle(session, restaurantId, title);
        if (menu == null)
        {
            session.close();
            throw new NotFoundException("Menu not found for restaurant id " + restaurantId);
        }
        AddMenuTitleRequest request = parseRequestBody(exchange, AddMenuTitleRequest.class);
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty())
        {
            session.close();
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "Invalid input: 'title' is required."));
            return;
        }

        menu.setTitle(request.getTitle());
        session.merge(menu);
        tx.commit();
        sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, new MenuTitleResponse(menu.getTitle()));
        session.close();
    }

    private void handleDeleteMenuTitle(HttpExchange exchange, User authenticatedUser, Integer restaurantId, String menuTitle) throws IOException {
        if (!checkHttpMethod(exchange, "DELETE")) return;
        if (!restaurantManager.isOwner(restaurantId, String.valueOf(authenticatedUser.getId()))) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Not the owner of this restaurant."));
            return;
        }
        if (!(authenticatedUser instanceof Seller) || !((Seller) authenticatedUser).isApproved()) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Seller is not yet approved to manage menus."));
            return;
        }
        menuManager.deleteMenuTitle(restaurantId, menuTitle, String.valueOf(authenticatedUser.getId()));
        sendResponse(exchange, HttpURLConnection.HTTP_OK, new ApiResponse(true, "Food menu removed from restaurant successfully"));
    }

    private void handleAddMenuItem(HttpExchange exchange, User authenticatedUser, Integer restaurantId, String menuTitle) throws IOException {
        if (!checkHttpMethod(exchange, "PUT")) return;
        if (!restaurantManager.isOwner(restaurantId, String.valueOf(authenticatedUser.getId()))) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Not the owner of this restaurant."));
            return;
        }
        if (!(authenticatedUser instanceof Seller) || !((Seller) authenticatedUser).isApproved()) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Seller is not yet approved to manage menus."));
            return;
        }
        AddMenuItemRequest request = parseRequestBody(exchange, AddMenuItemRequest.class);
        if (request == null || request.getItemId() == null) {
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "Invalid input: 'item_id' is required."));
            return;
        }
        menuManager.addItemToMenu(restaurantId, menuTitle, request.getItemId(), String.valueOf(authenticatedUser.getId()));
        sendResponse(exchange, HttpURLConnection.HTTP_OK, new ApiResponse(true, "Food item added to menu successfully"));
    }

    private void handleDeleteMenuItemFromMenu(HttpExchange exchange, User authenticatedUser, Integer restaurantId, String menuTitle, Integer itemId) throws IOException {
        if (!checkHttpMethod(exchange, "DELETE")) return;
        if (!restaurantManager.isOwner(restaurantId, String.valueOf(authenticatedUser.getId()))) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Not the owner of this restaurant."));
            return;
        }
        if (!(authenticatedUser instanceof Seller) || !((Seller) authenticatedUser).isApproved()) {
            sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, new ApiResponse(false, "Forbidden request: Seller is not yet approved to manage menus."));
            return;
        }
        menuManager.deleteItemFromMenu(restaurantId, menuTitle, itemId, String.valueOf(authenticatedUser.getId()));
        sendResponse(exchange, HttpURLConnection.HTTP_OK, new ApiResponse(true, "Item removed from restaurant menu successfully"));
    }

    private Optional<String> extractMenuTitleFromPath(String path, Pattern pattern) {
        Matcher matcher = pattern.matcher(path);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            String encodedValue = matcher.group(1);
            try {
                String decodedValue = URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
                return Optional.ofNullable(decodedValue);
            }  catch (Exception e) {
                System.err.println("Error decoding path variable: " + e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}