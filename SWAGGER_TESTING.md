# Swagger API testing

Swagger UI is the main interface for manually exploring and demonstrating this
API. The automated JUnit, Mockito and MockMvc tests remain the regression safety
net and should still be run before deployment.

## Start the application

```bash
./gradlew bootRun
```

Open `http://localhost:8080`. The root address redirects to Swagger UI. The
direct address is `http://localhost:8080/swagger-ui/index.html`.

Select **Try it out** on an operation, edit its parameters or JSON body, and
then select **Execute**. Swagger displays the request, response status, headers
and body.

## Public catalog

These operations do not require authentication:

1. `GET /api/categories`
2. `GET /api/products`
3. `GET /api/products/{id}`
4. `GET /api/products/search`

Use an ID returned by the list operations instead of assuming a fixed database
ID.

## Customer scenario

1. Run `POST /api/auth/login` with:

   ```json
   {
     "email": "customer@example.com",
     "password": "password123"
   }
   ```

2. Copy only the `token` value from the response.
3. Select **Authorize** at the top of Swagger UI, paste the token without adding
   the word `Bearer`, and confirm.
4. Run `GET /api/cart`.
5. Add a product with `POST /api/cart/items`, using an ID returned by
   `GET /api/products`:

   ```json
   {
     "productId": 1,
     "quantity": 2
   }
   ```

6. Create a pending order with `POST /api/orders`:

   ```json
   {
     "shippingAddress": "10 Bulgaria Blvd, Sofia",
     "items": [
       {
         "productId": 1,
         "quantity": 1
       }
     ]
   }
   ```

7. Copy the new order ID and create its mock payment with
   `POST /api/payments`:

   ```json
   {
     "orderId": 2
   }
   ```

## Administrator scenario

1. Run `POST /api/auth/login` with:

   ```json
   {
     "email": "admin@example.com",
     "password": "password123"
   }
   ```

2. Replace the customer token through **Authorize**.
3. Test the endpoints under the `Admin` sections, starting with
   `GET /api/admin/dashboard`.

## Expected authorization responses

- `401 Unauthorized`: the token is missing, invalid or expired.
- `403 Forbidden`: the token is valid, but the account does not have the
  required role.
- `400 Bad Request`: the JSON body or business operation is invalid.

## Automated verification

Run the complete regression suite before committing or deploying:

```bash
./gradlew test
```
