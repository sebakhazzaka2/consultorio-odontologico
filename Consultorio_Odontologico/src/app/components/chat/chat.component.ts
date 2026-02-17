import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ChatService, Mensaje } from '../../services/chat.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('mensajesContainer') private mensajesContainer!: ElementRef;
  @ViewChild('inputMensaje') private inputMensaje!: ElementRef;

  isOpen = false;
  mensajes: Mensaje[] = [];
  nuevoMensaje = '';
  private mensajesSubscription?: Subscription;
  private shouldScroll = false;

  constructor(public chatService: ChatService) {}

  ngOnInit(): void {
    this.mensajesSubscription = this.chatService.mensajes$.subscribe(mensajes => {
      this.mensajes = mensajes;
      this.shouldScroll = true;
    });
  }

  ngOnDestroy(): void {
    this.mensajesSubscription?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => {
        this.inputMensaje?.nativeElement?.focus();
      }, 100);
    }
  }

  enviarMensaje(): void {
    if (this.nuevoMensaje.trim()) {
      this.chatService.agregarMensaje(this.nuevoMensaje.trim());
      this.nuevoMensaje = '';
      this.shouldScroll = true;
    }
  }

  enviarConEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviarMensaje();
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.mensajesContainer) {
        const element = this.mensajesContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    } catch (err) {
      console.error('Error al hacer scroll:', err);
    }
  }

  formatearHora(fecha: Date): string {
    return new Date(fecha).toLocaleTimeString('es-ES', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }
}
