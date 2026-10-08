package com.zovira.order.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.ShippingAddress;
import com.zovira.order.repository.OrderRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates a GST tax invoice PDF for confirmed orders. */
@Service
public class InvoiceService {

    private static final Set<OrderStatus> INVOICEABLE = EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.RETURNED);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of("Asia/Kolkata"));
    private static final Color BRAND = new Color(0x15, 0x5A, 0x50);
    private static final Color MUTED = new Color(0x74, 0x73, 0x6B);

    private final OrderRepository orders;

    public InvoiceService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public byte[] invoice(Long userId, String orderNumber) {
        Order order = orders.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() -> NotFoundException.of("Order"));
        if (!INVOICEABLE.contains(order.getStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "An invoice is available once the order is confirmed");
        }
        return render(order);
    }

    byte[] render(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND);
        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

        PdfPTable top = new PdfPTable(new float[] {1, 1});
        top.setWidthPercentage(100);
        top.addCell(borderless(new Phrase("zovira", brand), Element.ALIGN_LEFT));
        PdfPCell title = borderless(new Phrase("Tax Invoice\nOrder " + order.getOrderNumber() + "\nDate " + DATE.format(order.getPlacedAt()), h), Element.ALIGN_RIGHT);
        top.addCell(title);
        doc.add(top);
        doc.add(new Paragraph(" "));

        ShippingAddress a = order.getShippingAddress();
        PdfPTable parties = new PdfPTable(new float[] {1, 1});
        parties.setWidthPercentage(100);
        String sellers = order.getItems().stream().map(i -> i.getSeller().getStoreName()
                + (i.getSeller().getGstin() != null ? " (GSTIN " + i.getSeller().getGstin() + ")" : "")).distinct()
                .reduce((x, y) -> x + "\n" + y).orElse("");
        parties.addCell(borderless(new Phrase("Sold by\n" + sellers, body), Element.ALIGN_LEFT));
        parties.addCell(borderless(new Phrase("Ship to\n" + a.getName() + "\n" + a.getLine1()
                + (a.getLine2() == null ? "" : ", " + a.getLine2()) + "\n" + a.getCity() + ", " + a.getState() + " "
                + a.getPincode() + "\nPhone " + a.getPhone(), body), Element.ALIGN_LEFT));
        doc.add(parties);
        doc.add(new Paragraph(" "));

        PdfPTable items = new PdfPTable(new float[] {4.5f, 1, 1.4f, 1.1f, 1.4f, 1.5f});
        items.setWidthPercentage(100);
        for (String col : new String[] {"Item", "Qty", "Unit price", "GST %", "GST amount", "Total"}) {
            PdfPCell c = new PdfPCell(new Phrase(col, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            c.setBackgroundColor(BRAND);
            c.setPadding(6);
            items.addCell(c);
        }
        for (OrderItem i : order.getItems()) {
            BigDecimal net = i.netAmount();
            BigDecimal gst = net.multiply(i.getTaxRate()).divide(i.getTaxRate().add(BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP);
            items.addCell(cell(i.getProductTitle() + (i.getVariantName() != null && !"Standard".equals(i.getVariantName()) ? "\n" + i.getVariantName() : "") + "\nSKU " + i.getSku(), body));
            items.addCell(cell(String.valueOf(i.getQuantity()), body));
            items.addCell(cell(money(i.getUnitPrice()), body));
            items.addCell(cell(i.getTaxRate().stripTrailingZeros().toPlainString() + "%", body));
            items.addCell(cell(money(gst), body));
            items.addCell(cell(money(net), body));
        }
        doc.add(items);
        doc.add(new Paragraph(" "));

        PdfPTable totals = new PdfPTable(new float[] {3, 1});
        totals.setWidthPercentage(50);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row(totals, "Items subtotal", money(order.getSubtotal()), body);
        if (order.getCouponDiscount().signum() > 0) {
            row(totals, "Coupon " + order.getCouponCode(), "-" + money(order.getCouponDiscount()), body);
        }
        row(totals, "Delivery", money(order.getShippingFee()), body);
        if (order.getCodFee().signum() > 0) {
            row(totals, "COD fee", money(order.getCodFee()), body);
        }
        row(totals, "Grand total", money(order.getTotalAmount()), h);
        row(totals, "GST included", money(order.getTaxAmount()), muted);
        doc.add(totals);
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + "). "
                + "Prices are inclusive of GST. This is a computer-generated invoice and does not require a signature.", muted));
        doc.close();
        return out.toByteArray();
    }

    private static String money(BigDecimal v) {
        return "Rs. " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static PdfPCell borderless(Phrase phrase, int align) {
        PdfPCell c = new PdfPCell(phrase);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(align);
        return c;
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(6);
        c.setBorderColor(new Color(0xE7, 0xE6, 0xE1));
        return c;
    }

    private static void row(PdfPTable t, String label, String value, Font font) {
        PdfPCell l = borderless(new Phrase(label, font), Element.ALIGN_LEFT);
        PdfPCell v = borderless(new Phrase(value, font), Element.ALIGN_RIGHT);
        l.setPadding(3);
        v.setPadding(3);
        t.addCell(l);
        t.addCell(v);
    }
}
