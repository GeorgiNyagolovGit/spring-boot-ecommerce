const state = {
    token: localStorage.getItem("marketplaceToken"),
    user: JSON.parse(localStorage.getItem("marketplaceUser") || "null"),
    categories: [],
    listings: [],
    conversations: [],
    activeConversationId: null,
    mineMode: false,
    authMode: "login"
};

const el = id => document.getElementById(id);
const conditionLabels = {
    NEW: "Ново",
    LIKE_NEW: "Като ново",
    GOOD: "Добро",
    FAIR: "Задоволително",
    FOR_PARTS: "За части"
};

async function api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body) headers["Content-Type"] = "application/json";
    if (state.token) headers.Authorization = `Bearer ${state.token}`;

    const response = await fetch(path, { ...options, headers });
    if (response.status === 204) return null;

    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        if (response.status === 401 || response.status === 403 && !state.user) {
            clearSession();
        }
        throw new Error(body?.message || "Възникна грешка. Опитай отново.");
    }
    return body;
}

function escapeHtml(value = "") {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function money(value) {
    return new Intl.NumberFormat("bg-BG", {
        style: "currency",
        currency: "BGN",
        minimumFractionDigits: Number(value) % 1 ? 2 : 0
    }).format(value || 0);
}

function shortDate(value) {
    if (!value) return "днес";
    return new Intl.DateTimeFormat("bg-BG", { day: "numeric", month: "short" })
        .format(new Date(value));
}

function showToast(message, error = false) {
    const toast = el("toast");
    toast.textContent = message;
    toast.classList.toggle("error", error);
    toast.classList.add("show");
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove("show"), 3200);
}

function openDialog(id) {
    const dialog = el(id);
    if (!dialog.open) dialog.showModal();
}

function closeDialog(id) {
    const dialog = el(id);
    if (dialog.open) dialog.close();
}

async function loadCategories() {
    state.categories = await api("/api/categories");
    const options = state.categories
        .map(category => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
        .join("");
    el("categorySelect").insertAdjacentHTML("beforeend", options);
    el("formCategory").insertAdjacentHTML("beforeend", options);
    el("categoryChips").innerHTML = state.categories
        .slice(0, 8)
        .map(category => `<button type="button" class="category-chip" data-category="${category.id}">${escapeHtml(category.name)}</button>`)
        .join("");
}

async function loadListings({ keyword = "", categoryId = "", mine = false } = {}) {
    el("listingGrid").innerHTML = loadingCards();
    el("emptyState").classList.add("hidden");
    state.mineMode = mine;

    try {
        let listings;
        if (mine) {
            listings = await api("/api/listings/mine");
        } else if (keyword.trim()) {
            listings = await api(`/api/listings/search?keyword=${encodeURIComponent(keyword.trim())}`);
            if (categoryId) listings = listings.filter(item => String(item.categoryId) === String(categoryId));
        } else if (categoryId) {
            listings = await api(`/api/listings/category/${categoryId}`);
        } else {
            listings = await api("/api/listings");
        }
        state.listings = listings;
        renderListings(listings, mine);
    } catch (error) {
        el("listingGrid").innerHTML = "";
        showToast(error.message, true);
        if (mine && !state.user) showAuth();
    }
}

function loadingCards() {
    return Array.from({ length: 8 }, () => `
        <article class="listing-card" aria-hidden="true">
            <div class="listing-image"></div>
            <div class="listing-body"><h3>Зареждане...</h3></div>
        </article>`).join("");
}

function renderListings(listings, mine) {
    el("catalogKicker").textContent = mine ? "Твоят профил" : "Нови предложения";
    el("catalogTitle").textContent = mine ? "Моите обяви" : "Последни обяви";
    el("listingCount").textContent = `${listings.length} ${listings.length === 1 ? "обява" : "обяви"}`;
    el("listingGrid").innerHTML = listings.map(item => listingCard(item, mine)).join("");
    el("emptyState").classList.toggle("hidden", listings.length !== 0);
}

function listingCard(item, mine) {
    const image = item.imageUrl
        ? `<img src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.name)}" loading="lazy">`
        : `<span aria-hidden="true">◈</span>`;
    return `
        <article class="listing-card" data-view="${item.id}" tabindex="0">
            <div class="listing-image">${image}</div>
            <div class="listing-body">
                <div class="listing-meta">
                    <span>${escapeHtml(item.location || item.categoryName || "България")}</span>
                    <span>${shortDate(item.createdAt)}</span>
                </div>
                <h3>${escapeHtml(item.name)}</h3>
                <div class="price-row">
                    <span class="price">${money(item.price)}</span>
                    ${item.condition ? `<span class="condition-badge">${conditionLabels[item.condition] || item.condition}</span>` : ""}
                </div>
            </div>
            ${mine ? `<div class="owner-actions">
                <button type="button" data-edit="${item.id}">Редактирай</button>
                <button type="button" class="delete-listing" data-delete="${item.id}">Свали</button>
            </div>` : ""}
        </article>`;
}

function showDetails(item) {
    const image = item.imageUrl
        ? `<img src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.name)}">`
        : "";
    el("detailContent").innerHTML = `
        <div class="detail-layout">
            <div class="detail-image">${image}</div>
            <div class="detail-copy">
                <p class="modal-kicker">${escapeHtml(item.categoryName || "Обява")} · ${escapeHtml(item.location || "България")}</p>
                <h2>${escapeHtml(item.name)}</h2>
                <div class="detail-price">${money(item.price)}</div>
                ${item.condition ? `<span class="condition-badge">${conditionLabels[item.condition] || item.condition}</span>` : ""}
                <p class="detail-description">${escapeHtml(item.description || "Няма допълнително описание.")}</p>
                <div class="seller-box">
                    <strong>${escapeHtml(item.sellerName || "Продавач")}</strong>
                    <p>Свържи се директно с продавача:</p>
                    ${item.contactPhone ? `<a href="tel:${escapeHtml(item.contactPhone)}">${escapeHtml(item.contactPhone)}</a>` : "Телефонът не е посочен"}
                    ${!state.user || Number(state.user.id) !== Number(item.sellerId)
                        ? `<button class="primary-button" type="button" data-message-listing="${item.id}">Напиши съобщение</button>`
                        : ""}
                </div>
            </div>
        </div>`;
    openDialog("detailDialog");
}

function updateAuthUi() {
    const loggedIn = Boolean(state.token && state.user);
    el("myListingsButton").classList.toggle("hidden", !loggedIn);
    el("messagesButton").classList.toggle("hidden", !loggedIn);
    el("authButton").textContent = loggedIn ? state.user.firstName || "Профил" : "Вход";
    el("loggedOutPanel").classList.toggle("hidden", loggedIn);
    el("accountPanel").classList.toggle("hidden", !loggedIn);
    if (loggedIn) {
        el("accountName").textContent = `${state.user.firstName || ""} ${state.user.lastName || ""}`.trim();
        el("accountEmail").textContent = state.user.email || "";
        el("accountAvatar").textContent = (state.user.firstName || "Н").charAt(0).toUpperCase();
    }
}

function saveSession(response) {
    state.token = response.token;
    state.user = {
        id: response.id,
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
        roles: response.roles
    };
    localStorage.setItem("marketplaceToken", state.token);
    localStorage.setItem("marketplaceUser", JSON.stringify(state.user));
    updateAuthUi();
    refreshConversationCount();
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.conversations = [];
    state.activeConversationId = null;
    localStorage.removeItem("marketplaceToken");
    localStorage.removeItem("marketplaceUser");
    updateUnreadBadge();
    updateAuthUi();
}

function showAuth() {
    updateAuthUi();
    openDialog("authDialog");
}

function setAuthMode(mode) {
    state.authMode = mode;
    const register = mode === "register";
    el("loginTab").classList.toggle("active", !register);
    el("registerTab").classList.toggle("active", register);
    el("nameFields").classList.toggle("hidden", !register);
    el("authTitle").textContent = register ? "Създай своя профил" : "Влез в профила си";
    el("authSubmit").textContent = register ? "Регистрация" : "Вход";
    el("authForm").elements.firstName.required = register;
    el("authForm").elements.lastName.required = register;
    el("authForm").elements.password.autocomplete = register ? "new-password" : "current-password";
    el("authError").classList.add("hidden");
}

function openListingForm(item = null) {
    if (!state.token) {
        showToast("Влез в профила си, за да публикуваш обява.");
        showAuth();
        return;
    }
    const form = el("listingForm");
    form.reset();
    form.elements.listingId.value = item?.id || "";
    form.elements.name.value = item?.name || "";
    form.elements.description.value = item?.description || "";
    form.elements.price.value = item?.price ?? "";
    form.elements.categoryId.value = item?.categoryId || "";
    form.elements.condition.value = item?.condition || "";
    form.elements.location.value = item?.location || "";
    form.elements.contactPhone.value = item?.contactPhone || "";
    form.elements.imageUrl.value = item?.imageUrl || "";
    el("listingFormTitle").textContent = item ? "Редактирай обявата" : "Публикувай нещо хубаво";
    el("listingFormError").classList.add("hidden");
    openDialog("listingFormDialog");
}

async function submitAuth(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    const endpoint = state.authMode === "register" ? "/api/auth/register" : "/api/auth/login";
    if (state.authMode === "login") {
        delete payload.firstName;
        delete payload.lastName;
    }
    try {
        const response = await api(endpoint, { method: "POST", body: JSON.stringify(payload) });
        saveSession(response);
        closeDialog("authDialog");
        event.currentTarget.reset();
        showToast(state.authMode === "register" ? "Профилът е създаден." : "Успешен вход.");
    } catch (error) {
        el("authError").textContent = error.message;
        el("authError").classList.remove("hidden");
    }
}

async function submitListing(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    const id = values.listingId;
    delete values.listingId;
    values.price = Number(values.price);
    values.stock = 1;
    values.categoryId = Number(values.categoryId);
    if (!values.imageUrl) values.imageUrl = null;

    try {
        await api(id ? `/api/listings/${id}` : "/api/listings", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(values)
        });
        closeDialog("listingFormDialog");
        showToast(id ? "Обявата е обновена." : "Обявата е публикувана.");
        await loadListings({ mine: true });
    } catch (error) {
        el("listingFormError").textContent = error.message;
        el("listingFormError").classList.remove("hidden");
    }
}

async function deleteListing(id) {
    if (!confirm("Да свалим ли тази обява?")) return;
    try {
        await api(`/api/listings/${id}`, { method: "DELETE" });
        showToast("Обявата е свалена.");
        await loadListings({ mine: true });
    } catch (error) {
        showToast(error.message, true);
    }
}

function openNewMessage(item) {
    if (!state.token) {
        closeDialog("detailDialog");
        showToast("Влез в профила си, за да пишеш на продавача.");
        showAuth();
        return;
    }
    if (Number(state.user?.id) === Number(item.sellerId)) {
        showToast("Това е твоята обява.");
        return;
    }

    const form = el("newMessageForm");
    form.reset();
    form.elements.listingId.value = item.id;
    const image = item.imageUrl
        ? `<img src="${escapeHtml(item.imageUrl)}" alt="">`
        : `<div class="conversation-image"></div>`;
    el("messageListingContext").innerHTML = `
        ${image}
        <div>
            <strong>${escapeHtml(item.name)}</strong>
            <span>${escapeHtml(item.sellerName || "Продавач")} · ${money(item.price)}</span>
        </div>`;
    el("newMessageError").classList.add("hidden");
    closeDialog("detailDialog");
    openDialog("newMessageDialog");
}

async function submitNewMessage(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget).entries());
    try {
        const conversation = await api("/api/conversations", {
            method: "POST",
            body: JSON.stringify({
                listingId: Number(values.listingId),
                message: values.message
            })
        });
        closeDialog("newMessageDialog");
        event.currentTarget.reset();
        showToast("Съобщението е изпратено.");
        await openMessages(conversation.id);
    } catch (error) {
        el("newMessageError").textContent = error.message;
        el("newMessageError").classList.remove("hidden");
    }
}

async function refreshConversationCount() {
    if (!state.token) return;
    try {
        state.conversations = await api("/api/conversations");
        updateUnreadBadge();
    } catch (error) {
        // The main page remains usable if the inbox cannot refresh.
    }
}

function updateUnreadBadge() {
    const total = state.conversations.reduce(
        (sum, conversation) => sum + Number(conversation.unreadCount || 0),
        0
    );
    el("unreadBadge").textContent = total > 99 ? "99+" : String(total);
    el("unreadBadge").classList.toggle("hidden", total === 0);
}

async function openMessages(selectedConversationId = null) {
    if (!state.token) {
        showAuth();
        return;
    }
    openDialog("messagesDialog");
    el("conversationList").innerHTML = "";
    el("conversationEmpty").classList.add("hidden");

    try {
        state.conversations = await api("/api/conversations");
        renderConversations();
        updateUnreadBadge();
        const targetId = selectedConversationId
            || state.activeConversationId
            || state.conversations[0]?.id;
        if (targetId) await openConversation(targetId);
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderConversations() {
    el("conversationEmpty").classList.toggle(
        "hidden",
        state.conversations.length !== 0
    );
    el("conversationList").innerHTML = state.conversations
        .map(conversation => {
            const image = conversation.listingImageUrl
                ? `<img class="conversation-image" src="${escapeHtml(conversation.listingImageUrl)}" alt="">`
                : `<div class="conversation-image"></div>`;
            return `
                <button class="conversation-item ${Number(conversation.id) === Number(state.activeConversationId) ? "active" : ""}"
                        type="button" data-conversation="${conversation.id}">
                    ${image}
                    <span class="conversation-copy">
                        <strong>${escapeHtml(conversation.otherUserName)}</strong>
                        <small>${escapeHtml(conversation.listingName)}</small>
                        <span>${escapeHtml(conversation.lastMessage || "Нов разговор")}</span>
                    </span>
                    ${conversation.unreadCount
                        ? `<span class="conversation-unread">${conversation.unreadCount}</span>`
                        : ""}
                </button>`;
        })
        .join("");
}

async function openConversation(conversationId) {
    const conversation = state.conversations.find(
        item => Number(item.id) === Number(conversationId)
    );
    if (!conversation) return;

    state.activeConversationId = Number(conversationId);
    renderConversations();
    el("threadPlaceholder").classList.add("hidden");
    el("activeThread").classList.remove("hidden");
    el("messageThread").innerHTML = `<p class="conversation-empty">Зареждане...</p>`;

    const image = conversation.listingImageUrl
        ? `<img class="thread-listing-image" src="${escapeHtml(conversation.listingImageUrl)}" alt="">`
        : `<div class="thread-listing-image"></div>`;
    el("threadHeader").innerHTML = `
        ${image}
        <div>
            <strong>${escapeHtml(conversation.otherUserName)}</strong>
            <span>${escapeHtml(conversation.listingName)}</span>
        </div>`;

    try {
        const messages = await api(
            `/api/conversations/${conversationId}/messages`
        );
        renderMessages(messages);
        conversation.unreadCount = 0;
        updateUnreadBadge();
        renderConversations();
    } catch (error) {
        el("messageThread").innerHTML = `<p class="conversation-empty">${escapeHtml(error.message)}</p>`;
    }
}

function renderMessages(messages) {
    el("messageThread").innerHTML = messages
        .map(message => `
            <div class="message-row ${message.sentByMe ? "mine" : ""}">
                <div class="message-bubble">
                    <p>${escapeHtml(message.body)}</p>
                    <time datetime="${escapeHtml(message.createdAt || "")}">${messageTime(message.createdAt)}</time>
                </div>
            </div>`)
        .join("");
    el("messageThread").scrollTop = el("messageThread").scrollHeight;
}

function messageTime(value) {
    if (!value) return "сега";
    return new Intl.DateTimeFormat("bg-BG", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

async function submitReply(event) {
    event.preventDefault();
    if (!state.activeConversationId) return;
    const form = event.currentTarget;
    const body = form.elements.body.value.trim();
    if (!body) return;

    try {
        await api(
            `/api/conversations/${state.activeConversationId}/messages`,
            { method: "POST", body: JSON.stringify({ body }) }
        );
        form.reset();
        el("replyError").classList.add("hidden");
        const messages = await api(
            `/api/conversations/${state.activeConversationId}/messages`
        );
        renderMessages(messages);
        state.conversations = await api("/api/conversations");
        renderConversations();
    } catch (error) {
        el("replyError").textContent = error.message;
        el("replyError").classList.remove("hidden");
    }
}

function startInboxPolling() {
    setInterval(async () => {
        if (!state.token || document.hidden) return;
        try {
            if (el("messagesDialog").open && state.activeConversationId) {
                const messages = await api(
                    `/api/conversations/${state.activeConversationId}/messages`
                );
                renderMessages(messages);
                state.conversations = await api("/api/conversations");
                const active = state.conversations.find(
                    item => Number(item.id) === state.activeConversationId
                );
                if (active) active.unreadCount = 0;
                renderConversations();
                updateUnreadBadge();
            } else {
                await refreshConversationCount();
            }
        } catch (error) {
            // Polling retries quietly on the next interval.
        }
    }, 15000);
}

function bindEvents() {
    document.querySelectorAll("[data-close]").forEach(button => {
        button.addEventListener("click", () => closeDialog(button.dataset.close));
    });
    document.querySelectorAll("dialog").forEach(dialog => {
        dialog.addEventListener("click", event => {
            if (event.target === dialog) dialog.close();
        });
    });

    el("searchForm").addEventListener("submit", event => {
        event.preventDefault();
        loadListings({ keyword: el("searchInput").value, categoryId: el("categorySelect").value });
        el("catalogTitle").scrollIntoView({ behavior: "smooth" });
    });
    document.querySelector(".category-strip").addEventListener("click", event => {
        const button = event.target.closest("[data-category]");
        if (!button) return;
        document.querySelectorAll(".category-chip").forEach(chip => chip.classList.remove("active"));
        button.classList.add("active");
        el("categorySelect").value = button.dataset.category;
        loadListings({ categoryId: button.dataset.category });
    });

    el("listingGrid").addEventListener("click", event => {
        const edit = event.target.closest("[data-edit]");
        const remove = event.target.closest("[data-delete]");
        if (edit) {
            event.stopPropagation();
            openListingForm(state.listings.find(item => String(item.id) === edit.dataset.edit));
            return;
        }
        if (remove) {
            event.stopPropagation();
            deleteListing(remove.dataset.delete);
            return;
        }
        const card = event.target.closest("[data-view]");
        if (card) showDetails(state.listings.find(item => String(item.id) === card.dataset.view));
    });
    el("listingGrid").addEventListener("keydown", event => {
        if (event.key === "Enter" && event.target.matches("[data-view]")) {
            event.target.click();
        }
    });
    el("detailContent").addEventListener("click", event => {
        const button = event.target.closest("[data-message-listing]");
        if (!button) return;
        const listing = state.listings.find(
            item => String(item.id) === button.dataset.messageListing
        );
        if (listing) openNewMessage(listing);
    });

    el("authButton").addEventListener("click", showAuth);
    el("publishButton").addEventListener("click", () => openListingForm());
    el("loginTab").addEventListener("click", () => setAuthMode("login"));
    el("registerTab").addEventListener("click", () => setAuthMode("register"));
    el("authForm").addEventListener("submit", submitAuth);
    el("listingForm").addEventListener("submit", submitListing);
    el("newMessageForm").addEventListener("submit", submitNewMessage);
    el("replyForm").addEventListener("submit", submitReply);
    el("conversationList").addEventListener("click", event => {
        const button = event.target.closest("[data-conversation]");
        if (button) openConversation(button.dataset.conversation);
    });
    el("logoutButton").addEventListener("click", () => {
        clearSession();
        closeDialog("authDialog");
        loadListings();
        showToast("Излезе от профила си.");
    });
    el("myListingsButton").addEventListener("click", () => loadListings({ mine: true }));
    el("messagesButton").addEventListener("click", () => openMessages());
    el("openMessagesFromAccount").addEventListener("click", () => {
        closeDialog("authDialog");
        openMessages();
    });
    el("openMyListings").addEventListener("click", () => {
        closeDialog("authDialog");
        loadListings({ mine: true });
    });
    el("allListingsButton").addEventListener("click", () => loadListings());
}

async function init() {
    updateAuthUi();
    bindEvents();
    startInboxPolling();
    try {
        await Promise.all([loadCategories(), loadListings()]);
        if (state.token) refreshConversationCount();
    } catch (error) {
        showToast("Сайтът не успя да зареди данните.", true);
    }
}

init();
