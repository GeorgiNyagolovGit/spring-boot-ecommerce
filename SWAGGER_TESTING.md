# Swagger testing guide

Open `http://localhost:8080/swagger-ui/index.html` after starting the API.

## Authenticate

1. Run `POST /api/auth/login` with `customer@example.com` and
   `password123`.
2. Copy the returned `token`.
3. Select **Authorize**, paste the token and confirm.

Google login can alternatively send the Google Identity Services ID token as
`credential` to `POST /api/auth/google`.

## Test the marketplace flow

1. Use `GET /api/listings` without authentication to browse ads.
2. Use `POST /api/listings` with:

```json
{
  "name": "Used city bicycle",
  "description": "Recently serviced and ready to ride",
  "price": 450,
  "stock": 1,
  "imageUrl": "https://example.com/bicycle.jpg",
  "categoryId": 1,
  "location": "Sofia",
  "condition": "GOOD",
  "contactPhone": "+359 888 123 456"
}
```

3. Run `GET /api/listings/mine` to see your own ads.
4. Use `PUT /api/listings/{id}` to edit an owned ad.
5. Use `DELETE /api/listings/{id}` to deactivate it.

Public search is available at
`GET /api/listings/search?keyword=sofia`. It searches the title, description
and location.

The API intentionally has no payment or Stripe endpoints.

## Test user messages

Use two different accounts because a seller cannot message their own listing.

1. Authenticate as the interested buyer.
2. Run `POST /api/conversations` with the listing ID and first message.
3. Run `GET /api/conversations` to list the inbox.
4. Use `GET /api/conversations/{conversationId}/messages` to open the chat.
5. Reply with `POST /api/conversations/{conversationId}/messages`:

```json
{
  "body": "Мога ли да го видя утре?"
}
```

Only the buyer and the listing owner can read or write in that conversation.
