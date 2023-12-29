package org.hft.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hft.data.PriceHolder;
import org.hft.dto.PriceDto;
import org.hft.task.PriceUpdateTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PriceReceiver {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PriceHolder priceHolder;
    private final ExecutorService executorService;

    public PriceReceiver(PriceHolder priceHolder, ExecutorService executorService) {
        this.priceHolder = priceHolder;
        this.executorService = executorService;
    }

    public void receivePrices() {
        try (DatagramSocket socket = new DatagramSocket(5000)) {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // deserialize to PriceDTO object
                String json = new String(buffer, 0, packet.getLength());
                PriceDto priceDto = objectMapper.readValue(json, PriceDto.class);
                System.out.println(Thread.currentThread().getName() + " - Price received: " + priceDto.toString());

                // start price update task
                executorService.submit(new PriceUpdateTask(priceHolder, priceDto));
            }
        } catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}