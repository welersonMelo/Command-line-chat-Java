package br.ufs.dcomp.chat;

import br.ufs.dcomp.chat.*;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.lang.*;
import com.rabbitmq.client.*;
import java.util.Scanner;
import java.io.IOException;

import java.nio.file.*;
import java.io.*;

import com.google.protobuf.ByteString; 

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
  
  public static String getFileName(String path){
      int i = path.length() - 1;
      while(i > 0 && path.charAt(i) != '/')
          i--;
      
      return path.substring(i+1, path.length());
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
    Channel channelFile = connection.createChannel();
    Channel channelText = connection.createChannel();
    
    //Inicializando chat, login
    Scanner sc = new Scanner(System.in); 
    System.out.println("");
    System.out.print("User: ");
    String user = sc.nextLine();
    System.out.println();
    
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(user+"-text", false,   false,     false,       null);
    channel2.queueDeclare(user+"-file", false,   false,     false,       null);
    
    // Replicar queue, uma para arquivo e outra para mensagem normal
    // Usar o rooting key do Rmq para rotear arquivos enviados para exchanges(grupos)

    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //Preparando para receber mensagem -------------------------------------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        //Mensagem recebida de algum dos usuário
        MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);        
        String emissor = message.getEmissor();
        String data = message.getData();
        String hora = message.getHora();
        String grupo = message.getGrupo();
        MensagemProto.Conteudo messageContent = message.getConteudo();
        
        //Se for uma mensagem de console
        if(messageContent.getTipo().equals("text/plain")){
            String header = "";
            if(grupo == ""){
                header = "("+ data + " às " + hora + ") @" + emissor + " diz: ";
            }else{
                header = "("+ data + " às " + hora + ") " + emissor + "#" + grupo +" diz: ";
            }
                
            String finalMessage = new String(messageContent.getCorpo().toByteArray(), "UTF-8");
            System.out.print("\n" + header + finalMessage);
        }//Caso seja um arquivo
        else{
            Scanner sci = new Scanner(System.in); 
            System.out.println("\n@" + emissor + " te enviou um arquivo. Deseja baixar? (Y/N)");
            String ans = sci.nextLine().trim();
            
            if(!(ans.equals("N") || ans.equals("n"))){
                String header = "("+ data + " às " + hora + ") Arquivo '" + messageContent.getNome() + "' recebido de @"+ emissor;
                System.out.print("\n" + header);
                
                byte[] fileReceived;
                fileReceived = messageContent.getCorpo().toByteArray();
                OutputStream os = new FileOutputStream("chat-file@"+emissor+"-"+messageContent.getNome());
                os.write(fileReceived);
                os.close();
            }
        }
        
        System.out.print("\n"+pretext+">>");
      }
    };
    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    
                       
                         //(queue-name, autoAck, consumer);    
    channel.basicConsume( user+"-text", true,   consumer);
    channel2.basicConsume( user+"-file", true,   consumer);
    
    String toSend, receiver = "", messageToSend = "", command = "", groupName = "", friendName = "";
    
    boolean readyToSend = false;
    FileInputStream file = null;
    File in = null;
    
    while(true){
        String filePath = "";
        boolean isMessageText = true;
        
        pretext = "";
        if(receiver.length() > 0){
            pretext += "@";
            readyToSend = true;
        }else if(groupName.length() > 0){
            pretext += "#";
            readyToSend = true;
        }
            
        pretext += receiver+groupName;
        System.out.print("\n"+pretext+">>");
        
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
                }else if(command.equals("upload")){
                    isMessageText = false;
                    filePath = toSend.substring(endPosiCommand, toSend.length()).trim();
                    in = new File(filePath);
	                file = null;
                    try { 
                        file = new FileInputStream(in); 
                        System.out.println("Uploading file " + filePath + " to " + pretext + "...");
                        readyToSend = true;  
                        break;
                    }catch(Exception e){
                        System.out.println("Erro ao tentar enviar aquivo. Arquivo não encontrado!");
                        readyToSend = false;  
                    }
                }else{
                    System.out.print("Comando não encontrado!");
                }
                readyToSend = false;    
                break;
            }
        }
        if(readyToSend){
             MensagemProto.Conteudo.Builder conteudoMen = MensagemProto.Conteudo.newBuilder();
            
            //Verificando tipo da mensagem
            if(isMessageText){
                //Se for texto no console
                ByteString bs = ByteString.copyFrom(toSend.getBytes());
                conteudoMen.setTipo("text/plain");
                conteudoMen.setCorpo(bs);
                conteudoMen.setNome("");
            }else{
                byte fileContent[] = new byte[(int)in.length()];
                file.read(fileContent);
                
                Path source = Paths.get(filePath);
                String tipoMime = Files.probeContentType(source);
                
                ByteString bs = ByteString.copyFrom(fileContent);
                conteudoMen.setTipo(tipoMime);
                conteudoMen.setCorpo(bs);
                conteudoMen.setNome(getFileName(filePath));
                
                file.close();
            }
            
            MensagemProto.Conteudo conteudoPronto = conteudoMen.build();
            
            //Preparando para serializar mensagem
            MensagemProto.Mensagem.Builder message = MensagemProto.Mensagem.newBuilder();
            message.setEmissor(user);
            message.setData(new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime()));
            message.setHora(new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
            message.setGrupo(groupName);
            message.setConteudo(conteudoPronto);
        
            //messageToSend 
            MensagemProto.Mensagem mensagemPronta = message.build();
        
            //Serializando 
            byte[] buffer = mensagemPronta.toByteArray(); 
           
            if(isMessageText){
                 //Evinando para a fila de texto do RMQ
                channel.basicPublish(groupName, receiver+"-text" , null, buffer);
            }else{
                //Evinando para a fila de arquivo do RMQ
                channel2.basicPublish("", receiver+"-file" , null, buffer);
            }
           
        }
    }
  }
}

// Criando um exchange: channel.exchangeDeclare("logs", "fanout");
// Dizendo pra qual fila o exchange irá redirecionar a mensagem: channel.queueBind(queueName, "logs", "");

