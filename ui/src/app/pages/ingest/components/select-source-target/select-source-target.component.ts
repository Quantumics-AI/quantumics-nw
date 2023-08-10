import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';

@Component({
  selector: 'app-select-source-target',
  templateUrl: './select-source-target.component.html',
  styleUrls: ['./select-source-target.component.css']
})
export class SelectSourceTargetComponent {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  public projectId: number;

  public fg: FormGroup;
  public fgt: FormGroup;

  public sourceData = [
    {id:1, name:'Bucket1'},
    {id:2, name:'Bucket2'},
    {id:3, name:'Bucket3'},
    {id:4, name:'Bucket4'},
    {id:5, name:'Bucket5'}
  ];

  public targetData = [
    {id:1, name:'Bucket1'},
    {id:2, name:'Bucket2'},
    {id:3, name:'Bucket3'},
    {id:4, name:'Bucket4'},
    {id:5, name:'Bucket5'}
  ];
  public queryFolders: boolean;

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
  ){
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
        }
      });
  }

  ngOnInit(): void {
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');

    this.fg = this.fb.group({
      // source: [false],
      source: new FormControl('', Validators.required),
    });

    this.fgt = this.fb.group({
      target: new FormControl('', Validators.required),
    });
  }

  public save(): void {
    this.snakbar.open("Saved succesfully");
    this.router.navigate([`projects/${this.projectId}/ingest/aws`]);
  }
}
