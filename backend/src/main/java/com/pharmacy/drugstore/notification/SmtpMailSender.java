package com.pharmacy.drugstore.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

class SmtpMailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    void send(String host, int port, String username, String password, String from,
              List<String> recipients, String subject, String body) throws IOException {
        Socket raw = new Socket();
        raw.connect(new InetSocketAddress(host, port), 15_000);
        raw.setSoTimeout(20_000);
        try {
            BufferedReader in = reader(raw);
            PrintWriter out = writer(raw);
            expect(read(in), 220);
            write(out, "EHLO localhost");
            expect(read(in), 250);
            write(out, "STARTTLS");
            expect(read(in), 220);

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket ssl = (SSLSocket) sslFactory.createSocket(raw, host, port, true);
            ssl.startHandshake();
            in = reader(ssl);
            out = writer(ssl);

            write(out, "EHLO localhost");
            expect(read(in), 250);
            write(out, "AUTH LOGIN");
            expect(read(in), 334);
            write(out, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
            expect(read(in), 334);
            write(out, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
            expect(read(in), 235);
            write(out, "MAIL FROM:<" + from + ">");
            expect(read(in), 250);
            for (String to : recipients) {
                write(out, "RCPT TO:<" + to + ">");
                expect(read(in), 250);
            }
            write(out, "DATA");
            expect(read(in), 354);
            out.print("From: " + from + "\r\n");
            out.print("To: " + String.join(", ", recipients) + "\r\n");
            out.print("Subject: =?UTF-8?B?"
                    + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                    + "?=\r\n");
            out.print("MIME-Version: 1.0\r\n");
            out.print("Content-Type: text/plain; charset=UTF-8\r\n");
            out.print("Content-Transfer-Encoding: 8bit\r\n\r\n");
            out.print(body.replace("\r\n", "\n").replace("\n", "\r\n"));
            out.print("\r\n.\r\n");
            out.flush();
            expect(read(in), 250);
            write(out, "QUIT");
            log.info("SMTP delivered via {}:{} to {}", host, port, recipients);
        } finally {
            try { raw.close(); } catch (IOException ignored) { }
        }
    }

    private static PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static void write(PrintWriter out, String line) {
        out.print(line + "\r\n");
        out.flush();
    }

    private static String read(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line).append('\n');
            if (line.length() < 4 || line.charAt(3) == ' ') {
                break;
            }
        }
        return sb.toString();
    }

    private static void expect(String response, int code) throws IOException {
        if (response == null || !response.startsWith(String.valueOf(code))) {
            throw new IOException("SMTP " + code + " expected, got: " + (response == null ? "empty" : response.trim()));
        }
    }
}
