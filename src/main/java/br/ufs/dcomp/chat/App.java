package br.ufs.dcomp.chat;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.lang.*;
import com.rabbitmq.client.*;
import java.util.Scanner;
import java.io.IOException;

public class App {
    
  static public String pretext = "";
  
  public static String addNewGroup(String toSend, int endPosiCommand){
      String groupName = "";
      try {
          groupName = toSend.substring(endPosiCommand+1, toSend.length());  
      } catch(Exception e) {
          System.err.println("Nome do grupo invalido tente novamente!");
      }
      return groupName;
  }
  
  public static String[] getUserGroup(String toSend, int endPosiCommand){
      String retorno[] = new String[2];
      String groupName = "", friendName = "";
      int endPosiName = 0;
      
      for(int k = endPosiCommand+1; k < toSend.length(); k++){
          if(toSend.charAt(k) == ' '){
              endPosiName = k;
              break;
          }
      }
      
      try {
          friendName = toSend.substring(endPosiCommand+1, endPosiName);
      } catch(Exception e) {
          System.err.println("Nome de usuário invalido tente novamente!");
      }
      
      try {
          groupName = toSend.substring(endPosiName+1, toSend.length());  
      } catch(Exception e) {
          System.err.println("Nome do grupo invalido tente novamente!");
      }
      
      retorno[0] = friendName;
      retorno[1] = groupName;
      
      return retorno;
      
  }

  public static void main(String[] argv) throws Exception {
    
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri("amqp://cyymdjjz:5APCLwN2DgBf2-I96_6Fw-fQ-qY_uTXm@emu.rmq.cloudamqp.com/cyymdjjz");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    //Inicializando chat, login
    Scanner sc = new Scanner(System.in); 
    System.out.println("");
    System.out.print("User: ");
    String user = sc.nextLine();
    System.out.println();
    
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(user, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        //Mensagem recebida de algum dos usuários
        String message = new String(body, "UTF-8");
        System.out.print("\n" + message + pretext);
      }
    };
    
                         //(queue-name, autoAck, consumer);    
    channel.basicConsume( user , true,    consumer);
    
    String toSend, receiver = "", messageToSend = "", command = "", groupName = "", friendName = "";
    
    boolean readyToSend = false;
    while(true){
        pretext = "\n";
        if(receiver.length() > 0){
            pretext += "@";
            readyToSend = true;
        }else if(groupName.length() > 0){
            pretext += "#";
            readyToSend = true;
        }
            
        pretext += receiver+groupName+">>";
        System.out.print(pretext);
        
        toSend = sc.nextLine();
        
        for(int i = 0; i < toSend.length(); i++){
            //Pre processamento texto 
            if(toSend.charAt(i) == '@'){
                groupName = "";
                receiver = toSend.substring(i+1, toSend.length());
                readyToSend = false;    
                break;
            }else if(toSend.charAt(i) == '#'){
                receiver = "";
                groupName = toSend.substring(i+1, toSend.length());
                readyToSend = false;    
                break;
            }else if(toSend.charAt(i) == '!'){
                int endPosiCommand = 0;
                for(int k = i; k < toSend.length(); k++){
                    if(toSend.charAt(k) == ' '){
                        endPosiCommand = k;
                        break;
                    }
                }
                
                try {
                    command = toSend.substring(i+1, endPosiCommand);  
                } catch(Exception e) {
                    System.err.println("Erro: Comando não encontrado ou faltando argumentos");
                }
                
                //Adicionando novo grupo
                if(command.equals("addGroup")){
                    try {    
                        groupName = addNewGroup(toSend, endPosiCommand);
                        channel.exchangeDeclare(groupName, "fanout");  
                        channel.queueBind(user, groupName, "");
                    } catch(Exception e) {
                        System.err.println("Erro ao tentar criar grupo");
                    };
                //Adicionando novo usuário num grupo
                }else if(command.equals("addUser")){
                    String[] aux = new String[2];
                    aux = getUserGroup(toSend, endPosiCommand);
                    
                    try {
                        friendName = aux[0];
                        groupName = aux[1];
                        
                        channel.queueBind(friendName, groupName, "");
                        
                    } catch(Exception e) {
                        System.err.println("Erro ao tentar add amigo no grupo");
                    };
                    groupName = "";
                }else if(command.equals("delFromGroup")){
                    String[] aux = new String[2];
                    aux = getUserGroup(toSend, endPosiCommand);
                    
                    try {
                        friendName = aux[0];
                        groupName = aux[1];
                        
                        channel.queueUnbind(friendName, groupName, "");
                        
                    } catch(Exception e) {
                        System.err.println("Erro ao tentar remover amigo do grupo");
                    };
                    groupName = "";
                }else if(command.equals("removeGroup")){
                    try {    
                        groupName = addNewGroup(toSend, endPosiCommand);
                        channel.exchangeDelete(groupName);  
                    } catch(Exception e) {
                        System.err.println("Erro ao tentar excluir grupo");
                    };
                    groupName = "";
                }else{
                    System.out.print("Comando não encontrado!");
                }
                
                readyToSend = false;    
                break;
            }
        }
        if(readyToSend){
            String toGroup = "";
            if(groupName.length() != 0){
                toGroup = "#";
            }
            
            String timeStamp = new SimpleDateFormat("(dd/MM/yyyy 'às' HH:mm)").format(Calendar.getInstance().getTime());
            
            messageToSend = timeStamp  + " " + user + toGroup + groupName + " diz: " + toSend;  
            
            //System.out.println(messageToSend);    
            channel.basicPublish(groupName, receiver , null,  messageToSend.getBytes("UTF-8"));
        }
    }
  }
}

// Criando um exchange: channel.exchangeDeclare("logs", "fanout");
// Dizendo pra qual fila o exchange irá redirecionar a mensagem: channel.queueBind(queueName, "logs", "");

