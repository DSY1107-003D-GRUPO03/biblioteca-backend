package cl.duoc.dsy1107.ms_rabbit_admin.service;

import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.duoc.dsy1107.ms_rabbit_admin.dto.QueueRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;

import cl.duoc.dsy1107.ms_rabbit_admin.dto.BindingRequest;
import cl.duoc.dsy1107.ms_rabbit_admin.dto.ExchangeRequest;
import cl.duoc.dsy1107.ms_rabbit_admin.exception.RecursoNoEncontradoException;

@Service 
@RequiredArgsConstructor 
public class RabbitAdminService {
    
    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    //Colas

    public QueueRequest crearCola(QueueRequest req) {
        QueueBuilder builder = req.durable()
                ? QueueBuilder.durable(req.name())
                : QueueBuilder.nonDurable(req.name());
    
        if (req.quorum()) {
            builder.quorum();
        }

        if (hayTexto(req.deadLetterExchange())) {
            builder.deadLetterExchange(req.deadLetterExchange());
        }

        if (hayTexto(req.deadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(req.deadLetterRoutingKey());
        }

        amqpAdmin.declareQueue(builder.build());
        return req;
    }

    public void eliminarCola(String nombre) {
        if (!existeCola(nombre)) {
            throw new RecursoNoEncontradoException("La cola no existe: " + nombre);
        }
        amqpAdmin.deleteQueue(nombre);
    }

    //Exchanges
    public ExchangeRequest crearExchange(ExchangeRequest req) {
        ExchangeBuilder builder = switch (req.type()) {
            case "direct" -> ExchangeBuilder.directExchange(req.name());
            case "topic" -> ExchangeBuilder.topicExchange(req.name());
            case "fanout" -> ExchangeBuilder.fanoutExchange(req.name());
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + req.type());
        };

        amqpAdmin.declareExchange(builder.durable(req.durable()).build());
        return req;
    } 

    public void eliminarExchange(String nombre) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange no existe: " + nombre);
        }
        amqpAdmin.deleteExchange(nombre);
    }

    //Bindgins
    public BindingRequest crearBinding(BindingRequest req) {
        validarQueExisten(req);
        amqpAdmin.declareBinding(construirBinding(req));
        return req;
    }

    public void eliminarBinding(BindingRequest req) {
        validarQueExisten(req);
        amqpAdmin.removeBinding(construirBinding(req));
    }


    //METODOS AUXILIARES

    private boolean existeCola(String nombre) {
        return amqpAdmin.getQueueInfo(nombre) != null;
    }

    private boolean existeExchange(String nombre) {
        try {
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> {
                channel.exchangeDeclarePassive(nombre);
                return true;
            }));
        } catch (AmqpIOException e) {
            return false;
        }
    }

    private void validarQueExisten(BindingRequest req) {
        if (!existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("El exchange no existe: " + req.exchange());
        }

        if (!existeCola(req.queue())) {
            throw new RecursoNoEncontradoException("La cola no existe: " + req.queue());
        }

    }

    private Binding construirBinding(BindingRequest req) {
        return new Binding(req.queue(), Binding.DestinationType.QUEUE,
                req.exchange(), req.routingKey(), null);
    }



    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

}
