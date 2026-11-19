# Skylink-Airline-Reservation-System
Creating repo for Airline Ticket Resevation System

## Traveler booking and payment

The traveler booking, payment, and submission pages use a responsive Skylink theme. Booking keeps passenger details when the count changes, validates the visible passenger fields, and previews promotions using stored discount values. The booking subtotal includes selected extras and any applied promotion. Checkout then adds the existing Rs. 45 payment fee.

The traveler can submit a card or eZ Cash payment. Submission creates a **PENDING** payment and leaves the booking **PENDING**. A Finance Executive can approve it, changing the payment to **COMPLETED** and the booking to **CONFIRMED**, or reject it, changing the payment to **FAILED** and the booking to **CANCELLED**. The submission page shows this review process; it does not represent a completed charge or issued ticket.

Payment page access is restricted to the booking owner. The submitted payment amount must match the stored booking total plus the Rs. 45 fee. The legacy `/traveler-payment` URL redirects to the checked `/api/payment/page` route and ignores query string amounts. Existing POST routes, payment methods, CSRF protection, and finance role checks remain in place.

### Known limitations

- Card and eZ Cash submissions are recorded for finance review; the traveler flow does not integrate a live payment gateway, send an OTP, issue an e-ticket, or provide a downloadable PDF.
- Traveler extras use the approved per-passenger prices shown in the booking form. The server validates each submitted selection and calculates the extras total; the hidden `bookingExtras` and `totalPrice` values do not set the charge. The selected extras are not stored individually in the current database schema, so staff cannot independently verify each entitlement from a booking record.
- Promotion redemption now requires an authenticated traveler, an explicitly active scope, and valid Colombo calendar dates (end date inclusive). `GENERAL` is available to every traveler; `TARGETED` requires explicit membership in `promotion_users`. Legacy promotions with null scope or activation remain unavailable until authorized marketing or IT staff classify and activate them. `targetCriteria` is descriptive only. See [the manual migration plan](docs/promotion-authorization-migration.md) before starting against an existing MySQL database.
- The confirmation page receives booking details through redirect flash attributes. Opening it directly shows a link to My Bookings for the current status.
- Automated tests cover booking creation, pricing, pending payment, duplicate prevention, ownership, amount integrity, CSRF and role boundaries, and finance decisions. Browser layout and keyboard checks remain manual.
