import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BrandLogoComponent } from './brand-logo.component';

describe('BrandLogoComponent', () => {
  let fixture: ComponentFixture<BrandLogoComponent>;
  let component: BrandLogoComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrandLogoComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(BrandLogoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the SVG shield', () => {
    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg).toBeTruthy();
  });

  it('applies size md (28px) by default', () => {
    const svg: HTMLElement = fixture.nativeElement.querySelector('svg');
    expect(svg.style.width).toBe('28px');
    expect(svg.style.height).toBe('28px');
  });

  it('applies size lg (40px) when input is lg', () => {
    component.size = 'lg';
    fixture.detectChanges();
    const svg: HTMLElement = fixture.nativeElement.querySelector('svg');
    expect(svg.style.width).toBe('40px');
  });

  it('applies size sm (20px) when input is sm', () => {
    component.size = 'sm';
    fixture.detectChanges();
    const svg: HTMLElement = fixture.nativeElement.querySelector('svg');
    expect(svg.style.width).toBe('20px');
  });

  it('applies size xl (64px) when input is xl', () => {
    component.size = 'xl';
    fixture.detectChanges();
    const svg: HTMLElement = fixture.nativeElement.querySelector('svg');
    expect(svg.style.width).toBe('64px');
  });

  it('hides wordmark by default', () => {
    const wordmark = fixture.nativeElement.querySelector('.wordmark');
    expect(wordmark).toBeNull();
  });

  it('shows wordmark when showWordmark is true', () => {
    component.showWordmark = true;
    fixture.detectChanges();
    const wordmark = fixture.nativeElement.querySelector('.wordmark');
    expect(wordmark).toBeTruthy();
    expect(wordmark.textContent).toContain('Nexa');
    expect(wordmark.textContent).toContain('Clinic');
  });
});
