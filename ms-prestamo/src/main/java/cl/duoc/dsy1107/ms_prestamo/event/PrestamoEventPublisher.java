package cl.duoc.dsy1107.ms_prestamo.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrestamoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String rkCreado;
    private final String rkDevuelto;

    public PrestamoEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbit.exchange}") String exchange,
            @Value("${app.rabbit.rk-creado}") String rkCreado,
            @Value("${app.rabbit.rk-devuelto}") String rkDevuelto) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.rkCreado = rkCreado;
        this.rkDevuelto = rkDevuelto;
    }

    public void publicarCreado(PrestamoCreadoEvent evento) {
        rabbitTemplate.convertAndSend(exchange, rkCreado, evento);
    }

    public void publicarDevuelto(PrestamoDevueltoEvent evento) {
        rabbitTemplate.convertAndSend(exchange, rkDevuelto, evento);
    }
}